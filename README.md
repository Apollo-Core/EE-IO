![Apollo-Core CI Java Repository](https://github.com/Apollo-Core/EE-IO/workflows/Apollo-Core%20CI%20Java%20Repository/badge.svg)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f67b612a45ef4b228092b8b2ef5932b6)](https://app.codacy.com/gh/Apollo-Core/EE-IO?utm_source=github.com&utm_medium=referral&utm_content=Apollo-Core/EE-IO&utm_campaign=Badge_Grade_Settings)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/55477bfc89dd4bcf8a365a0e9f53ab86)](https://www.codacy.com/gh/Apollo-Core/EE-IO/dashboard?utm_source=github.com&utm_medium=referral&utm_content=Apollo-Core/EE-IO&utm_campaign=Badge_Coverage)
[![](https://jitpack.io/v/Apollo-Core/EE-IO.svg)](https://jitpack.io/#Apollo-Core/EE-IO)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# EE-IO
Repository containing the components responsible for reading in the orchestration input (describing the processed workflow, the available resources, and the application input) and processing the orchestration output (the result of the application).

## Relevance

### Repository relevant if

+ You want to use Apollo for application orchestration ([EE-Demo](https://github.com/Apollo-Core/EE-Demo)) is probably a good place to start; see below for a description of the format of the input files required by Apollo)
+ You want to create parsers for new input formats
+ You want to extend or modify the existing input format for the definition of the workflow (AFCL, see the [explanation](https://apollowf.github.io/learn.html) and the [source code](https://github.com/Apollo-AFCL/AFCLCore-AFCLv1.1)), the available resources (JSON - see below), and/or the input data (JSON - see below)

### Repository less relevant if

+ You want implement a particular type of component, such as a scheduler (see [SC-Core](https://github.com/Apollo-Core/SC-Core)) or a new way of enacting functions


## Relations to other parts of Apollo-Core

### Depends On
+ EE-Core
+ EE-Guice
+ EE-Model

### Used By
+ EE-Visualization
+ EE-Demo
+ EE-Deploy


## Input File Format

This section describes the content and the format of the files which can be used with the Apollo run-time system.

### Input Data

The input data for processing is defined in JSON format. See the *inputData* directory in [EE-Demo](https://github.com/Apollo-Core/EE-Demo) for example files.

### Workflow Definition

The application workflow is defined with an [AFCL](https://apollowf.github.io/learn.html) file in YAML format.

### Function Type Mappings

The function type mapping file is defined in JSON format and describes the resources that can be used to process functions of particular types.

At the moment, Apollo supports following resource types:

+   **Serverless:** Serverless functions deployed by cloud providers (e.g., AWS Lambda or IBM Actions)
+   **Local Container:** Local execution (execution on the machine running corresponding Apollo instance) using Docker containers, provided via DockerHub. With these resources, a single task is processed by (I) starting the container, (II) executing the functions with the task input, and (III) removing the container. The images of all local-container resources are pulled during Apollo's configuration phase.
+   **Local Server:** Local execution (execution on the machine running corresponding Apollo instance) using Docker containers, provided via DockerHub. In this case, the Docker image contains a server with a Rest API which can be directly used to execute the corresponding function. The server is started up during the configuration phase of Apollo and is then accessible via HTTP requests.

#### Type Mapping Examples

The following example defines three possible type mappings for functions of the type _Addition_: (a) A serverless functions which is accessible under the link FUNCTION_LINK, (b) a function executed in a local container with the image IMAGE_NAME, (c) a function implemented within Apollo's source code (this is used exclusively to enable a demonstration of Apollo which relies neither on Docker containers nor on the deployment of serverless functions). 

```json
[
	{
		"functionType": "Addition",
		"resources": [
			{
				"type": "Serverless",
				"properties": {
					"Uri": "FUNCTION_LINK"
				}
			},
            {
				"type": "Local",
				"properties": {
					"Image": "IMAGE_NAME"
				}
			},
			{
				"type": "Demo",
				"properties": {					
				}
			}
		]
	}
]
```
