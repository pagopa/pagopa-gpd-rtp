# Test util

👀 Test util module, it executes the following app

- Redis container
- Kafka to Redis connector

## How run on Docker 🐳

To run the test util on docker, you can run from this directory the script:

``` shell
sh ./run_compose.sh <evh-host> <evh-conn-string> <evh-topics>
```