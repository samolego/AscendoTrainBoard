#!/bin/bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi.yaml \
  -g kotlin \
  -o app/composeApp/ \
  --global-property models \
  --package-name io.github.samolego.ascendo_trainboard.api.generated \
  --additional-properties=library=multiplatform,useCoroutines=true,dateLibrary=string
