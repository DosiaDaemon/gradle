/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.tasks.diagnostics

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class TaskTreeIntegrationTest extends AbstractIntegrationSpec {

    def "shows simple tree of tasks"() {
        given:
        settingsFile """rootProject.name = 'my-root'"""
        buildFile """
            def leaf1 = tasks.register("leaf1")
            def leaf2 = tasks.register("leaf2")
            def middle = tasks.register("middle"){
                dependsOn(leaf1, leaf2)
            }
            tasks.register("root"){
                dependsOn(leaf1, middle)
            }
        """

        when:
        succeeds("root", "--task-tree")

        then:
        outputContains("""
Tasks graph for: root
\\--- :root
     +--- :leaf1
     \\--- :middle
          +--- :leaf1 (*)
          \\--- :leaf2

(*) - details omitted (listed previously)
""")
    }

    def "shows simple tree of tasks with multiple roots"() {
        given:
        settingsFile """rootProject.name = 'my-root'"""
        buildFile """
            def leaf1 = tasks.register("leaf1")
            def leaf2 = tasks.register("leaf2")
            def middle = tasks.register("middle"){
                dependsOn(leaf1, leaf2)
            }
            tasks.register("root"){
                dependsOn(leaf1, middle)
            }
            tasks.register("root2"){
                dependsOn(leaf2)
            }
        """

        when:
        succeeds("root", "r2", "--task-tree")

        then:
        outputContains("""
Tasks graph for: root r2
+--- :root
|    +--- :leaf1
|    \\--- :middle
|         +--- :leaf1 (*)
|         \\--- :leaf2
\\--- :root2
     \\--- :leaf2 (*)

(*) - details omitted (listed previously)
""")
    }
}
