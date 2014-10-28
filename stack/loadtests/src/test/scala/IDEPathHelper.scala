/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import scala.tools.nsc.io.File
import scala.tools.nsc.io.Path.string2path

object IDEPathHelper {

	val gatlingConfUrl = getClass.getClassLoader.getResource("gatling.conf").getPath
	val projectRootDir = File(gatlingConfUrl).parents(2)

	val mavenSourcesDirectory = projectRootDir / "src" / "test" / "scala"
	val mavenResourcesDirectory = projectRootDir / "src" / "test" / "resources"
	val mavenTargetDirectory = projectRootDir / "target"
	val mavenBinariesDirectory = mavenTargetDirectory / "test-classes"

	val dataDirectory = mavenResourcesDirectory / "data"
	val requestBodiesDirectory = mavenResourcesDirectory / "request-bodies"

	val recorderOutputDirectory = mavenSourcesDirectory
	val resultsDirectory = mavenTargetDirectory / "results"

	val recorderConfigFile = (mavenResourcesDirectory / "recorder.conf").toFile
}