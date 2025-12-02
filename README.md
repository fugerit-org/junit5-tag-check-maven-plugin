# junit5-tag-check-maven-plugin

A simple plugin to check the presence of JUnit5 test by tags.

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](CHANGELOG.md) 
[![Maven Central](https://img.shields.io/maven-central/v/org.fugerit.java/junit5-tag-check-maven-plugin.svg)](https://central.sonatype.com/artifact/org.fugerit.java/junit5-tag-check-maven-plugin)
[![license](https://img.shields.io/badge/License-Apache%20License%202.0-teal.svg)](https://opensource.org/licenses/Apache-2.0)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fugerit-org_junit5-tag-check-maven-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fugerit-org_junit5-tag-check-maven-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fugerit-org_junit5-tag-check-maven-plugin&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fugerit-org_junit5-tag-check-maven-plugin)

## Configuration reference

| parameter                | type    | required | notes                                                      |
|--------------------------|---------|----------|------------------------------------------------------------|
| format                   | string  | false    | default 'txt', accepts 'html', 'json', 'xml', 'pdf', 'txt' |
| outputFile               | string  | true     | path where should be produced the report                   |
| requiredTags.requiredTag | string  | true     | tag to be checked                                          |
| failOnMissingTag         | boolean | true     | if set to 'true' the build will fail on missing tags       |

Here is a sample configuration

```
          <plugin>
            <groupId>org.fugerit.java</groupId>
            <artifactId>junit5-tag-check-maven-plugin</artifactId>
            <version>${junit5-tag-check-maven-plugin-version}</version>
            <executions>
              <execution>
                <id>report-executed-test-tags</id>
                <phase>verify</phase>
                <goals>
                  <goal>report-executed-tags</goal>
                </goals>
                <configuration>
                  <format>html</format>
                  <outputFile>${project.build.directory}/executed-test-tag-report.html</outputFile>
                  <requiredTags>
                    <requiredTag>security</requiredTag>
                    <requiredTag>authorized</requiredTag>
                    <requiredTag>unauthorized</requiredTag>
                    <requiredTag>forbidden</requiredTag>
                  </requiredTags>
                  <failOnMissingTag>true</failOnMissingTag>
                </configuration>
              </execution>
            </executions>
          </plugin>
```