/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.doc.server.rest.domain;

import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintCreator;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.internal.kernel.api.Kernel;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.api.security.AnonymousContext;
import org.neo4j.server.database.Database;

import static org.neo4j.graphdb.Label.label;
import static org.neo4j.helpers.collection.Iterables.count;
import static org.neo4j.helpers.collection.Iterables.single;
import static org.neo4j.internal.kernel.api.Transaction.Type.implicit;
import static org.neo4j.internal.kernel.api.security.LoginContext.AUTH_DISABLED;

public class GraphDbHelper
{
    private final Database database;

    public GraphDbHelper( Database database )
    {
        this.database = database;
    }

    public int getNumberOfNodes()
    {
        Kernel kernel = database.getGraph().getDependencyResolver().resolveDependency( Kernel.class );
        try ( org.neo4j.internal.kernel.api.Transaction tx = kernel.beginTransaction( implicit, AnonymousContext.read() ) )
        {
            return Math.toIntExact( tx.dataRead().nodesGetCount() );
        }
        catch ( TransactionFailureException e )
        {
            throw new RuntimeException( e );
        }
    }

    public int getNumberOfRelationships()
    {
        Kernel kernel = database.getGraph().getDependencyResolver().resolveDependency( Kernel.class );
        try ( org.neo4j.internal.kernel.api.Transaction tx = kernel.beginTransaction( implicit, AnonymousContext.read() ) )
        {
            return Math.toIntExact( tx.dataRead().relationshipsGetCount() );
        }
        catch ( TransactionFailureException e )
        {
            throw new RuntimeException( e );
        }
    }

    public Map<String, Object> getNodeProperties( long nodeId )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Node node = database.getGraph().getNodeById( nodeId );
            Map<String, Object> allProperties = node.getAllProperties();
            tx.success();
            return allProperties;
        }
    }

    public void setNodeProperties( long nodeId, Map<String, Object> properties )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node node = database.getGraph().getNodeById( nodeId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                node.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.success();
        }
    }

    public long createNode( Label... labels )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node node = database.getGraph().createNode( labels );
            tx.success();
            return node.getId();
        }
    }

    public long createNode( Map<String, Object> properties, Label... labels )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node node = database.getGraph().createNode( labels );
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
            tx.success();
            return node.getId();
        }
    }

    public void deleteNode( long id )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.write() ) )
        {
            Node node = database.getGraph().getNodeById( id );
            node.delete();
            tx.success();
        }
    }

    public long createRelationship( String type, long startNodeId, long endNodeId )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node startNode = database.getGraph().getNodeById( startNodeId );
            Node endNode = database.getGraph().getNodeById( endNodeId );
            Relationship relationship = startNode.createRelationshipTo( endNode, RelationshipType.withName( type ) );
            tx.success();
            return relationship.getId();
        }
    }

    public long createRelationship( String type )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node startNode = database.getGraph().createNode();
            Node endNode = database.getGraph().createNode();
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    RelationshipType.withName( type ) );
            tx.success();
            return relationship.getId();
        }
    }

    public void setRelationshipProperties( long relationshipId, Map<String, Object> properties )

    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                relationship.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.success();
        }
    }

    public Map<String, Object> getRelationshipProperties( long relationshipId )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            Map<String, Object> allProperties = relationship.getAllProperties();
            tx.success();
            return allProperties;
        }
    }

    public Relationship getRelationship( long relationshipId )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            tx.success();
            return relationship;
        }
    }

    public long getFirstNode()
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.write() ) )
        {
            try
            {
                Node referenceNode = database.getGraph().getNodeById( 0L );

                tx.success();
                return referenceNode.getId();
            }
            catch ( NotFoundException e )
            {
                Node newNode = database.getGraph().createNode();
                tx.success();
                return newNode.getId();
            }
        }
    }

    public Iterable<String> getNodeLabels( long node )
    {
        return new IterableWrapper<String, Label>( database.getGraph().getNodeById( node ).getLabels() )
        {
            @Override
            protected String underlyingObjectToObject( Label object )
            {
                return object.name();
            }
        };
    }

    public void addLabelToNode( long node, String labelName )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            database.getGraph().getNodeById( node ).addLabel( label( labelName ) );
            tx.success();
        }
    }

    public Iterable<IndexDefinition> getSchemaIndexes( String labelName )
    {
        return database.getGraph().schema().getIndexes( label( labelName ) );
    }

    public IndexDefinition createSchemaIndex( String labelName, String propertyKey )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AUTH_DISABLED ) )
        {
            IndexDefinition index = database.getGraph().schema().indexFor( label( labelName ) ).on( propertyKey ).create();
            tx.success();
            return index;
        }
    }

    public Iterable<ConstraintDefinition> getPropertyUniquenessConstraints( String labelName, final String propertyKey )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Iterable<ConstraintDefinition> definitions = Iterables.filter( item ->
            {
                if ( item.isConstraintType( ConstraintType.UNIQUENESS ) )
                {
                    Iterable<String> keys = item.getPropertyKeys();
                    return single( keys ).equals( propertyKey );
                }
                else
                {
                    return false;
                }

            }, database.getGraph().schema().getConstraints( label( labelName ) ) );
            tx.success();
            return definitions;
        }
    }

    public ConstraintDefinition createPropertyUniquenessConstraint( String labelName, List<String> propertyKeys )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( implicit, AUTH_DISABLED ) )
        {
            ConstraintCreator creator = database.getGraph().schema().constraintFor( label( labelName ) );
            for ( String propertyKey : propertyKeys )
            {
                creator = creator.assertPropertyIsUnique( propertyKey );
            }
            ConstraintDefinition result = creator.create();
            tx.success();
            return result;
        }
    }

    public long getLabelCount( long nodeId )
    {
        try ( Transaction transaction = database.getGraph().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            return count( database.getGraph().getNodeById( nodeId ).getLabels());
        }
    }
}