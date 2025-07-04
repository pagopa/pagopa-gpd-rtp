# Integration Tests

👀 Integration tests are in `integration-test/src/` folder. See there for more information.

## Technology Stack

- [cucumber js](https://github.com/cucumber/cucumber-js)
- NodeJS v14.17.6

## How to start

- install dependencies: `yarn install`
- run tests: `yarn test`

if all right you should see something like that :

```sh
3 scenarios (3 passed)
26 steps (26 passed)
1m25.240s (executing steps: 1m20.434s)
┌──────────────────────────────────────────────────────────────────────────┐
│ View your Cucumber Report at:                                            │
│ https://reports.cucumber.io/reports/16ebc4c0-cab6-41f6-9355-f894f9a9601d │
│                                                                          │
│ This report will self-destruct in 24h.                                   │
│ Keep reports forever: https://reports.cucumber.io/profile                │
└──────────────────────────────────────────────────────────────────────────┘
```

Click on reporter link to view details .

### Debug

To run a single _feature_ or single _Scenario_ typing

Ex. single _features_ `organizations.feature`

```sh
npx cucumber-js -r step_definitions features/<filename>.feature
```

Ex. single _Scenario_ into `<filename>.feature` ( add source line )

```sh
npx cucumber-js -r step_definitions features/<filename>.feature:46
```

### Note

Remember to start the Backend before start the tests.

You can configure the host in `./config/.env.local` file.

## How run on Docker 🐳

To run the integration tests on docker, you can run from this directory the script:

``` shell
sh ./run_integration_test.sh <local|dev|uat|prod>
```

---
💻 If you want to test your local branch,

``` shell
sh ./run_integration_test.sh local
```