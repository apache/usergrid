Generating Coverage reports
---

To generate Jacoco coverage reports for Usergrid, run this command in the usergrid/stack directory:

    mvn verify jacoco:report

Note that all tests must pass, or some Jacoco files will be missing and report generation is likely to fail complete.

Once you do that the below coverage reports will be available.

Coverage reports
---

* [./core/target/site/jacoco/index.html](file:./core/target/site/jacoco/index.html)
* [./corepersistence/collection/target/site/jacoco/index.html](file:./corepersistence/collection/target/site/jacoco/index.html)
* [./corepersistence/common/target/site/jacoco/index.html](file:./corepersistence/common/target/site/jacoco/index.html)
* [./corepersistence/graph/target/site/jacoco/index.html](file:./corepersistence/graph/target/site/jacoco/index.html)
* [./corepersistence/map/target/site/jacoco/index.html](file:./corepersistence/map/target/site/jacoco/index.html)
* [./corepersistence/model/target/site/jacoco/index.html](file:./corepersistence/model/target/site/jacoco/index.html)
* [./corepersistence/queryindex/target/site/jacoco/index.html](file:./corepersistence/queryindex/target/site/jacoco/index.html)
* [./corepersistence/queue/target/site/jacoco/index.html](file:./corepersistence/queue/target/site/jacoco/index.html)
* [./services/target/site/jacoco/index.html](file:./services/target/site/jacoco/index.html)
* [./rest/target/site/jacoco/index.html](file:./services/target/site/jacoco/index.html)

Master Coverage report
---

After you run the above command to generate Jacoco reports, you can run this command to generate a master report:

    mvn -f jacoco-pom.xml install

The master report will be available at this link:

* [./target/coverage-report/html/index.html](file:./target/coverage-report/html/index.html)

