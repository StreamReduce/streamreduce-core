# Prerequisites #

You will need the following installed and available on your PATH:

- JDK 7
- Maven 3.x

# Getting Started With StreamReduce Core #

You can interact with your StreamReduce instance by taking advantage of its REST API. We're using `curl` to illustrate how the API works.

# Login #

Before you can do anything meaningful with the API, you have to authenticate. Once you successfully authenticate, you'll pass the returned `X-Auth-Token` with each subsequent request. 

## Request ##

    > curl -XPOST -D - --user "[Your StreamReduce Username]:[Your StreamReduce Password]" --basic http://localhost:8080/webapp/authentication/login 

## Response ##

    HTTP/1.1 204 No Content
    Date: Thu, 20 Sep 2012 18:26:36 GMT
    Set-Cookie: JSESSIONID=ff0d30af-5d74-47cc-986c-8d981ee7cf34; Path=/webapp; HttpOnly
    Set-Cookie: rememberMe=deleteMe; Path=/webapp; Max-Age=0; Expires=Wed, 19-Sep-2012 18:26:36 GMT
    X-Auth-Token: aHfqkEpq9aa1Rcg0UIyPgmF9FinP8Ahz0ThgCbvD/AU=
    Content-Type: application/json
    Server: Jetty(8.0.4.v20111024)

We're interested in the value from the `X-Auth-Token` header. That's what we'll use in subsequent requests to perform other operations.

# Account and User Creation #

The default account and user will allow you to get started creating your own accounts and users. Creating an account first allows you to create users and connections which belong to that account. First, create a new account: 

    > curl -XPOST --data "@create_account.json" -H "Content-Type:application/json" -H "X-Auth-Token: eiSgi3SyxmAO4IL4h/b3mX3bsubawJ3vvTgCn8HfUpI=" http://localhost:8080/webapp/admin/account

The `create_account.json` payload is: 

    {
        "name" : "My Example Account",
        "url" : "http://www.example.com",
        "description" : "Example account for API testing"
    }

Only `name` is required. The other fields are optional. If successful, the command will return payload similar to: 

    {
        "id": "5060e462fa5a53d46dde7f15",
        "created": null,
        "modified": null,
        "version": 0,
        "name": "My Example Account",
        "url": "http://www.example.com",
        "description": "Example account for API testing",
        "fuid": "myexampleaccount_1348527202521"
    }

Once you have an account to work with, you can create a user account. The user account payload, referenced below as `create_user.json`, looks like the following:

	{
	    "username" : "myusername@example.com",
		"fullname" : "Example User",
	    "password" : "mypassword",
		"role" : "admin"
	}

# Connection Management #

Connections allow you to get data into StreamReduce for real-time processing. Connections can either be polled or, which means the StreamReduce application will periodically query the connection for data, or push. Push connections simply accept data on a dedicated REST endpoint, which we'll describe later.

## Listing Connections ##

To get a listing of the connections you've defined, perform a GET request against the `/api/connections` endpoint:

    > curl -XGET -D - -H "X-Auth-Token: eiSgi3SyxmAO4IL4h/b3mX3bsubawJ3vvTgCn8HfUpI=" http://localhost:8080/webapp/api/connections

This will return a JSON payload with your currently defined connections. 

## Creating Connections ##

To create a connection, you need to pass a JSON payload to the connections endpoint. It's easiest if you put the payload into a file and reference the file from the command line. To create a Pingdom connection, the following payload would be used: 

    {
        "alias": "Pingdom",
        "authType": "USERNAME_PASSWORD_WITH_API_KEY",
        "credentials": {
            "identity": "[Your Pingdom Username]",
            "credential": "[Your Pingdom Password]",
            "api_key": "[Your Pingdom API Key]"
        },
        "hashtags": ["pingdom"],
        "providerId": "pingdom",
        "visibility": "ACCOUNT"
    }

More information about specific connection types can be found [here](http://somelinktoconnectiontypes.com).

Once you have your connection defined, you can pass it to `curl`:

    > curl -XPOST --data "@create_pingdom_connection.json" -H "X-Auth-Token: eiSgi3SyxmAO4IL4h/b3mX3bsubawJ3vvTgCn8HfUpI=" -H "Content-Type:application/json" http://localhost:8080/webapp/api/connections/

The response contains the JSON representation of the newly created connection.

