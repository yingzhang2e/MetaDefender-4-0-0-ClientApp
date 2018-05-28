# This is based on Metadefender Core Client https://github.com/OPSWAT/Metadefender-Core-Client
> Java library for the Metadefender Core v4 REST API.

## OPSWAT Metascan 3.x.x and Metadefender Core v4 REST API
* Metascan 3.x.x is widely used in a lot of web applications for scan viruss when files are imported. 
However, beginning with Metascan 3.8.1, OPSWAT move out Java sample from https://portal.opswat.com/en/content/metadefender-core-sample-code and does not fully support for a Java application to use “omsJInterfaceImple.dll”

* OPSWAT suggests to use and Metadefender Core v4 REST API that only using the JSON-based (v2) REST API and XML-based (v1) REST API is deprecated

## Why do I add this application sample?
* https://github.com/OPSWAT/Metadefender-Core-Client gave good examples but
* It used Gradle and some developers are use Maven not Gradle
* Its samples used com.github.tomakehurst.wiremock.client.WireMock.*

## How to use Metadefender Core v4 REST API?
>This project will give instruction for a Java developer to use metadefender-core-client easier

Steps:
1. Go to https://www.opswat.com/ and "Start Free Trial"
2. Sign in  your demo account (http://localhost:8008 user:admin password:admin) and active your key for test
3. To activate key, navigate to Settings -> License then click "ACTIVATE"
4. Download metascan 4.x.x (for me its is ometascan-4.10.1-1-x64)
5. Intsall metascan by click your download
6. Import this project
7. Download metadefender-core-client-4.0.0.jar and add jar as External Libraries 
8. Download jackson-annotations-2.7.5.jar, jackson-core-2.7.5.jar, jackson-databind-2.7.5 and add them as External Libraries 
   These 3 jars are for compiling com.opswat.metadefender.core.client.MetadefenderCoreClient.java*
9. Go through com.opswat.metadefender.core.service.MetaDefenderClientService and find its methods. you may copy this and use in your project
10. Go through MetaDefenderClientServiceTest.java to see how MetaDefenderClientService will be used

> com.opswat.metadefender.core.client.MetadefenderCoreClient.java* is copied from https://github.com/OPSWAT/Metadefender-Core-Client 
  
## Metadefender Core Client Features

This library makes it easy to:
* Connect to a Metadefender Core API point
* Scan files for threats
* Retrieve previous file scan results by file hash, or data_id
* Login / Logout from the REST point
* Fetching Available Scan Rules
* Fetching Engine/Database Versions
* Get Current License Information
* Get the version of the Metadefender Core
