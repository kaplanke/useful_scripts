Example for updating the custom field value of a task in project online by using REST API.

Prerequisites;

 | |  |
---|---
PROJECT_GUID |the guid of the project that contains the task<br>
TASK_GUID | the guid of the task to be updated<br>
CUSTOM_FIELD_INTERNAL_NAME | the internal name of the custom field to be updated which can be obtained from the "/CustomFields" endpoint appanded to task URI. Someting like Custom_...<br>
ACCESS_TOKEN | pre obtained access key. (https://docs.microsoft.com/en-us/azure/devops/integrate/get-started/authentication/oauth?view=azure-devops)
<br>

1- checkout the project
```
curl --location --request POST 'https://xxx.sharepoint.com/sites/yyy/_api/ProjectServer/Projects(guid'\''$PROJECT_GUID'\'')/checkOut?access_token=$ACCESS_TOKEN' \
--header 'Accept: application/json;odata=verbose' \
--header 'Content-Type: application/json' \
--data-raw ''
```

2- update custom field value

```
curl --location --request POST 'https://xxx.sharepoint.com/sites/yyy/_api/ProjectServer/Projects(guid'\''$PROJECT_GUID'\'')/Draft/Tasks(guid'\''$TASK_GUID'\'')/UpdateCustomFields?access_token=$ACCESS_TOKEN' \
--header 'Accept: application/json;odata=verbose' \
--header 'Content-Type: application/json;odata=verbose' \
--data-raw '{
    "customFieldDictionary": [
        {
            "Key": "$CUSTOM_FIELD_INTERNAL_NAME",
            "Value": "The New Value",
            "ValueType": "Edm.String"
        }
    ]
}'
```

3- publish and checkIn the project

```
curl --location --request POST 'https://xxx.sharepoint.com/sites/yyy/_api/ProjectServer/Projects(guid'\''$PROJECT_GUID'\'')/draft/publish(true)?access_token=$ACCESS_TOKEN' \
--header 'Accept: application/json;odata=verbose' \
--header 'Content-Type: application/json' \
--data-raw ''
```
