![Apollo-Core CI Java Repository](https://github.com/Apollo-Core/EE-IO/workflows/Apollo-Core%20CI%20Java%20Repository/badge.svg)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f67b612a45ef4b228092b8b2ef5932b6)](https://app.codacy.com/gh/Apollo-Core/EE-IO?utm_source=github.com&utm_medium=referral&utm_content=Apollo-Core/EE-IO&utm_campaign=Badge_Grade_Settings)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/55477bfc89dd4bcf8a365a0e9f53ab86)](https://www.codacy.com/gh/Apollo-Core/EE-IO/dashboard?utm_source=github.com&utm_medium=referral&utm_content=Apollo-Core/EE-IO&utm_campaign=Badge_Coverage)
[![](https://jitpack.io/v/Apollo-Core/EE-IO.svg)](https://jitpack.io/#Apollo-Core/EE-IO)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# EE-IO
Project defining the components for the IO, i.e., the processing of workflow and data input and the output of the processing results, of the Apollo-Core Enactment Engine.

## Input Files

This section describes the content and the format of the files which can be used with the Apollo run-time system.

### Input Data

The input data for processing is defined in JSON format.


### Workflow Definition

The application workflow is defined with an AFCL file in YAML format.


### Function Type Mappings

The function type mapping file is defined in JSON format and describes the resources that can be used to process functions of particular types.

At the moment, Apollo supports following resource types:

+ Serverless functions deployed by cloud providers (e.g., AWS Lambda or IBM Actions)
+ Local execution (execution on the machine running corresponding Apollo instance) using Docker containers, provided via DockerHub.

#### Type Mapping Examples

The following example defines two possible type mappings for functions of the type _Addition_: (a) A serverless functions which is accessible under the link FUNCTION_LINK and (b) a function executed in a local container with the image IMAGE_NAME. 

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
			}
		]
	}
]
```
