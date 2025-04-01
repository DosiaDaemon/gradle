/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.execution;

import org.gradle.TaskExecutionRequest;
import org.gradle.api.Incubating;
import org.gradle.api.internal.GradleInternal;
import org.gradle.execution.plan.FinalizedExecutionPlan;
import org.gradle.execution.plan.Node;
import org.gradle.execution.plan.TaskDependencyResolver;
import org.gradle.internal.build.ExecutionResult;
import org.gradle.internal.graph.DirectedGraph;
import org.gradle.internal.graph.DirectedGraphRenderer;
import org.gradle.internal.graph.GraphNodeRenderer;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.gradle.internal.logging.text.StyledTextOutputFactory;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link BuildWorkExecutor} that disables all selected tasks before they are executed.
 */
@Incubating
public class TaskTreeBuildExecutionAction implements BuildWorkExecutor {
    private final StyledTextOutputFactory textOutputFactory;
    private final List<TaskExecutionRequest> taskRequests;

    public TaskTreeBuildExecutionAction(
        StyledTextOutputFactory textOutputFactory,
        List<TaskExecutionRequest> taskRequests
    ) {
        this.textOutputFactory = textOutputFactory;
        this.taskRequests = taskRequests;
    }

    @Override
    public ExecutionResult<Void> execute(GradleInternal gradle, FinalizedExecutionPlan plan) {
        plan.getContents().getScheduledNodes().visitNodes((nodes, entryNodes) -> {
            DirectedGraphRenderer<Node> renderer = new DirectedGraphRenderer<>(new NodeRenderer(), new NodesGraph());
            StyledTextOutput output = textOutputFactory.create("Task tree");
            renderer.renderTo(new RootNode(entryNodes, getInvocation()), output);
        });
        return ExecutionResult.succeeded();
    }

    private String getInvocation() {
        return taskRequests.stream()
            .map(TaskExecutionRequest::getArgs)
            .flatMap(List::stream)
            .collect(Collectors.joining(" "));
    }

    private static class NodeRenderer implements GraphNodeRenderer<Node> {

        @Override
        public void renderTo(Node node, StyledTextOutput output) {
            output.withStyle(StyledTextOutput.Style.Identifier).text(node.toString());
        }
    }

    private static class NodesGraph implements DirectedGraph<Node, Node> {

        @Override
        public void getNodeValues(Node node, Collection<? super Node> values, Collection<? super Node> connectedNodes) {
            values.add(node);
            for (Node child : node.getAllSuccessors()) {
                connectedNodes.add(child);
            }
        }
    }

    private static class RootNode extends Node {
        private final Collection<Node> entryNodes;
        private final String invocation;

        public RootNode(Collection<Node> entryNodes, String invocation) {
            this.entryNodes = entryNodes;
            this.invocation = invocation;
        }

        @Override
        @SuppressWarnings("MissingSuperCall")
        public Iterable<Node> getAllSuccessors() {
            return entryNodes;
        }

        @Override
        public @Nullable Throwable getNodeFailure() {
            return null;
        }

        @Override
        public void resolveDependencies(TaskDependencyResolver dependencyResolver) {
            // should not be used
        }

        @Override
        public String toString() {
            return "Tasks graph for: " + invocation;
        }
    }
}
