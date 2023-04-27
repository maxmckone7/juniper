#!/bin/sh
# populates an enrollee in development environment
# populate_enrollee.sh [portalShortcode] [envName] [studyShortcode] [filePath from populate/src/main/resources/seed]
# e.g. populate_enrollee.sh ourhealth sandbox ourheart portals/ourhealth/studies/ourheart/enrollees/jsalk.json
SERVER_NAME="localhost:8080"
set -u
# Fake access token for dbush@broadinstitute.org, generated by CurrentUnauthedUserService.generateFakeJwtToken
ACCESS_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJlbWFpbCI6ImRidXNoQGJyb2FkaW5zdGl0dXRlLm9yZyIsInRva2VuIjoiNzJjZDQyZTgtMWE0ZS00Nzg5LWFmODQtZDIwMDgxY2JlNWJjIn0."
# uncomment the below line to use a real Azure B2C token  (needed for populating anywhere other than localhost)
# ACCESS_TOKEN=$(az account get-access-token | jq -r .accessToken)
curl -X POST -H "Authorization: Bearer $ACCESS_TOKEN" "$SERVER_NAME/api/internal/v1/populate/enrollee/$1/env/$2/study/$3?filePathName=$4&overwrite=true"

echo ""
