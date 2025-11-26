# unit5-tag-check-maven-plugin

A simple plugin to check the presence of JUnit5 test by tags.

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](CHANGELOG.md) 
[![Maven Central](https://img.shields.io/maven-central/v/org.fugerit.java/unit5-tag-check-maven-plugin.svg)](https://central.sonatype.com/artifact/org.fugerit.java/unit5-tag-check-maven-plugin)
[![license](https://img.shields.io/badge/License-Apache%20License%202.0-teal.svg)](https://opensource.org/licenses/Apache-2.0)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fugerit-org_unit5-tag-check-maven-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fugerit-org_unit5-tag-check-maven-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fugerit-org_unit5-tag-check-maven-plugin&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fugerit-org_unit5-tag-check-maven-plugin)

Accepted config params are :  
* configPath
* idCatalog

Here a sample configuration  :

```
			<plugin>
				<groupId>org.fugerit.java</groupId>
				<artifactId>unit5-tag-check-maven-plugin</artifactId>
				<version>${openapi-doc-version}</version>	
				<configuration>
					<configPath>src/config/openapi-doc-config.xml</configPath>
					<idCatalog>openapi</idCatalog>		
				</configuration>							
				<executions>
					<execution>
						<id>openapi</id>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>		
				</executions>
			</plugin>	
```