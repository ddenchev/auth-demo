# Auth Service 

### Overview 

This service aims to provide JWT based Authentication and Authorization capabilities for other 
services deployed within the same VPC. It is particularly useful for applications running 
Kotlin/Ktor services due to the included Ktor authorization plugin.

The current state of the project is very much a build in progress. There are some key features 
missing such as asymmetric cryptography, refresh tokens, a UI, a complete set of client modules as 
well as finish the tests suit and documentation.  

### Domain Model 

There are five types of entities managed by this service. 

![Domain Model Diagram](./docs/domain_model.png)

##### App

An app represents an application under management by the Auth service. Each application has a 
distinct set of users. If two applications need to share users with each other, they need to be 
configured as the same app, even if technically speaking they are the same app. For example, the 
uber driver app and client app would be a single app from the point of view of the Auth service. 

##### User 

The user entity models an application user. A user can sign up, log in and otherwise manage 
their account as necessary. A user may be assigned a "role" which determines their level of 
access in the system. 

##### Resource

A resource is any entity (or set of related entities) that are managed anywhere within the 
ecosystem. The domain entities described in this readme are all modeled as resources in the auth 
service. 

A resource exists independently of any application. For example, the "image thumbnail" service 
can be modeled as a resource and used by multiple applications that may require thumbnails. 
In order for a resource to be available for use in an application, it must be "allocated" to the 
application.  

##### Permission 

A permission models the "right to perform a particular action on a resource". Each resource has 
an enumerated set of permissions. For example, the "user" resource, has a "list" permission.   

##### Role 

A role is a group of permissions that maybe granted to a user. Roles should be designed to 
describe the actual roles being performed by a user. For example, in an uber app, there may be a 
client role and a driver role. 

Roles can be assigned to a user, but it is entirely up to the app itself to decide how to manage 
role assignments. A typical, example might be an "manager" granting an employee a type of role, 
or a role being granted automatically upon a user completing a set of actions. 


### Structure of Codebase

The repository contains the following compilation modules:

| Module               | Type      | Targets    | Description                                                                                                                         |
|----------------------|-----------|------------|-------------------------------------------------------------------------------------------------------------------------------------|
| server               | Container | Docker     | Contains the bulk of the logic dealing with Auth, including database schema.                                                        |
| client               | Lib       | JVM, JS/TS | This is the client library for the auth service. It targets normal "login" and "signup" use cases available to a typical client.    |
| admin-client         | Lib       | JVM, JS/TS | The admin library focuses on app admin management tasks such as creating resources, managing users, etc...                          | 
| app-client           | Lib       | JVM, JS/TS | Targets the auth app itself. This client includes all capabilities of the admin client and can be used for any of the managed apps. |
| authorization-plugin | Lib       | JVM        | A Ktor plugin that provides a stock implementation of securing a Ktor route in one line.                                            |
| shared               | Lib       | none       | A repo internal library containing dataclasses that are shared between the server and the client.                                   |
| shared-test-fixtures | Lib       | none       | Test fixtures that assist in testing.                                                                                               |       

To build the project:

**0. ENV / Configuration**

This project uses env variables to configure certain project secrets and env configurations. In 
development, you will need to create a top level `.env` file. The file should be populated with 
the variables in the [.env.template](.env.template) file commited to this repository. 

**1. Start postgres**

The server component uses Jooq to generate typesafe data classes for all SQL tables. To perform 
the analysis however, it needs a running postgres instance at build time. 

Most conveniently postgres can be started via docker and the included docker-compose file.

```shell
docker-compose up auth-db
```

**2. Build the project**

Build the project via the included ./gradlew instance. The a cold build may take some time, but 
during development the build cache allows recompilation within a few seconds. 

```shell
./gradlew build
```

**(optional) Run the tests**

```shell
./gradlew test
```

**3. Start the server**

```shell
./gradlew :server:run
```

During an initial run of the server the "auth" app will register itself with AppID  000..00 and 
all resources under management will be configured.  

You should now be able to access the API. 

To login as the root user:

```shell
curl -X 'POST' \
  'http://localhost:8080/apps/00000000-0000-0000-0000-000000000000/login' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "username": "root",
  "password": "password"
}'
```

You should be able to receive a response that looks like this:

```shell
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwOi8vMC4wLjAuMDo4MDgwLyIsImF1ZCI6IjAwMDAwMDAwLTAwMDAtMDAwMC0wMDAwLTAwMDAwMDAwMDAwMCIsImV4cCI6MTczNDA5NDQ4MiwidWlkIjoiZjQ1ZDhlMjktYzRiMC00NzU2LThiNmQtNTM3YWVkOWZhMDY5IiwicHJtIjoiQUFBQUFBQUgvLzQ9In0.YjBy6sSK0e4BTfFeCSn4Xvkf6QIhB08gusSCU5Wx8DA"
}
```

The `token` is a JWT encoding all permissions available to the root user, that can be decoded 
via any standard JWT decoding utility (two base64 encoded parts).

### Structure of the JWT 

Decoding the token above, it contains the following parts in its payload. 

```json 
{
  "iss": "http://0.0.0.0:8080/",
  "aud": "00000000-0000-0000-0000-000000000000",
  "exp": 1734094482,
  "uid": "f45d8e29-c4b0-4756-8b6d-537aed9fa069",
  "prm": "AAAAAAAH//4="
}
```

- `iss`  
  - The iss claim is set to the value in `JWT_ISSUER`. It would be normal to set it to something 
    identifying the issuing service.
- `aud`
  - The audience claim is set to the appId where the user is authenticated. 
- `exp`
  - The expiration timestamp. After this time, the token should be considered invalid. 
- `uid`
  - The user uuid to whom this JWT applies.  
- `prm`
  - The `prm` custom claim contains the user's current set of permissions. The encoding schema 
    for the `prm` claim is described below. 

##### Permission Encoding Schema

The JWT standard is quite flexible and doesn't provide any specific mechanism for expressing 
permissions. A common way to configure the JWT is a with a custom claim containing an explicit 
list of all permissions. For example `["resource:read", "app:create",...]`. There are two 
problems with this setup:
1. There is limited space in JWT. Most HTTP headers cannot grow beyond 8kb. 
2. Any payload weight added to the JWT will also be added to every future request, thus 
   increasing network traffic. 

This implementation minimizes the size of the token by compressing in the following way:
- Every permission has a "permission key", which is an integer allocated in increasing fashion. 
- The permission key represents the bit, in a permission bit mask allocated for that permission. 
- At encoding time, a bit mask is constructed with all permissions that are "on" and base64 
  encoded into the JWT. 
- At decoding time, the resource server uses the allocate permission key to determine if the 
  user has access. 

Reference:
- Encoding: [LoginUser.kt](server/src/main/kotlin/doichin/auth/services/user/LoginUser.kt)
- Permission Check: [CallingUser.kt](authorization-plugin/src/main/kotlin/doichin/auth/plugins/authorization/CallingUser.kt)

### Server - Code Organization 

The bulk of the implementation is contained in the server module. At the top level there are two 
files [Application.kt](server/src/main/kotlin/doichin/auth/Application.kt) and 
[EnvType.kt](server/src/main/kotlin/doichin/auth/EnvType.kt). 

`EnvType` is simply an enum listing the available environments. `Application.kt` on the other 
hand contains all of the app initialization logic. A fair bit of the initialization is also in the 
[plugins](server/src/main/kotlin/doichin/auth/plugins) folder, which contains Ktor specific 
plugin configurations used in the project. 

The [routes](server/src/main/kotlin/doichin/auth/routes) folder contains code defining all 
available API endpoints. Code at this level deals with HTTP specific concerns such parsing 
parameters, encoding responses and http route documentation. 

The [services](server/src/main/kotlin/doichin/auth/services) folder contains classes that 
implement the real business logic. In most cases, services are responsible for ensuring data 
integrity by managing transactions between multiple repositories.  

The [repositories](server/src/main/kotlin/doichin/auth/repositories) folder contains data access 
logic. Internally it leverages Jooq for typesafe data access. However, repositories do not 
expose the jooq entities to callers, instead always operating on well understood domain objects. 

The [resources.db.migration](server/src/main/resources/db/migration) folder contains a list of 
all migrations defining the set of db tables in use. The migrations are managed by flyway and 
follow the flyway naming convention. 

