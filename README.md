# SUM Calendaring
### Server Under the Mountain (SUM) Group
Open source solution for secure and highly scalable calendar, notes, and tasks.

## Meet the Team
This project was developed at [The University of North Carolina at Greensboro](https://www.uncg.edu/) as a [Computer Science Senior Capstone](https://www.uncg.edu/cmp/) project, Spring 2018.
<table style="width:100%">
  <tr>
    <a href="https://www.linkedin.com/in/collinguarino/"><img src="https://avatars3.githubusercontent.com/u/3580553?s=460&v=4" width="100"> Collin Guarino @collinux </a>
  </tr>
  <tr>
    <a href="https://www.linkedin.com/in/hahnalex/"><img src="https://avatars2.githubusercontent.com/u/7029850?s=400&v=4" width="100"> Alex Hahn @hahnalex </a>
  </tr>
  <tr>
     <a href=""><img src="https://avatars2.githubusercontent.com/u/15807171?s=400&v=4" width="100"> Jessica Denney @jessicadenney </a>
  <tr>
  <tr>
     <a href="https://www.linkedin.com/in/luke-roosje-4199a713a/"><img src="https://avatars0.githubusercontent.com/u/17089635?s=400&v=4" width="100"> Luke Roosje @leroosje </a>
  <tr>
  <tr>
     <a href=""><img src="https://avatars1.githubusercontent.com/u/18103250?s=460&v=4" width="100"> Michael Burke @maburke </a>
  <tr>
</table>
 

## Problem Statement
As a user or organization I cannot easily host a cryptographically secure server to host calendaring data. Existing solutions
are either proprietary and therefore not easily auditable or they require extensible configuration with a background in IT infrastructure.

## Solution Statement
1. At the end of this project there will be a user-friendly way to host a cryptographically secure calendaring server to use with any calendar front-end application. 
2. It will also provide the extensibility to scale the service to large organizations.

## Features
 * Entirely free and open source
 * End to end encryption
 * OAuth & 2-Factor Authentication
 * Zero access architecture means that the data can only be decrypted by the user and not the organization hosting the service
 * Complete anonymizzation of metadata, no IP addresses or visitor history logged, accessible over Tor
 * Easily auditable since it's made entirely from open source cryptography tools
 * Calendar events, notes, and tasks that dissappear from all invited guests after a specified amount of time
 * Compatable with existing calendaring applications like outlook, thunderbird, evolution, and others by using the [iCalendar Spec RFC 5545](https://en.wikipedia.org/wiki/ICalendar) standard
 * [Docker](https://www.docker.com/what-docker) enabled for portability and ease of use for users with minimal technical experience
 * [Kuberneties](https://kubernetes.io/docs/concepts/overview/what-is-kubernetes/) enabled for infinitely scalable internal IT infrastructure or external services

## Architecture Overview
1. REST API lets clients authenticate using [OAuth](https://oauth.net/2/) (standard username and password setting) then lets them request their data from a server or update their data on a server. This REST API is hosted by the server and talks to a [SQL](https://en.wikipedia.org/wiki/SQL) server.
    * REST API can be developed in nearly any language and there are numerous libraries that make this simple. [Here](http://cherrypy.org/) is an example of a Python library that lets you host a REST API in as little as 5 lines.
2. The SQL server accesses the user's data and decrypts it with their private key which is transferred over SSL and not stored.
3. The API on the REST server responds to the client in a specific format, with the standard for calendar events being [iCalendar Spec RFC 5545](https://en.wikipedia.org/wiki/ICalendar). "Notes" and "Tasks" are just calendar events without due dates... many front end applications able to handle these differences.

The result of these three primary components is a service that will allow any user to access a secure calendar, notes, or tasks source from any app (google calendar, canvas calendar, apple, yahoo, thunderbird, evolution, outlook, etc). 

## What are we going to demonstrate in the presentation?
We are not developing a front-end, we will use an existing application (such as Outlook) to prove that our infrastructure works. A simple landing page such as [this](https://protonmail.com/) may be a nice visual to explain our security features and be the centerpiece of our final poster. We can make a simple diagram outlining the architecture of this project: a client talking to a server, the user authentication process, the server using its API to fetch user data from a database, and the server responding back to the client with data. Further explanation on the tools we used for each component have high value in the presentation.

## Roles & Components 
1. Simple web design using [MDL (material design lite)](https://mdl.io) which will be shown in the poster presentation - see [landing page example](https://protonmail.com/). No hosting experience needed since GitHub does that for us with [GitHub pages](https://pages.github.com). Languages: HTML and CSS.

2. REST API development & networking: understand how servers work, how to make http calls and get a response. Must learn the [iCalendar Spec RFC 5545](https://en.wikipedia.org/wiki/ICalendar), serialize/deserialize .ics files in [JSON](https://www.w3schools.com/js/js_json_intro.asp).

3. Security: use open source libraries to implement [OAuth](https://oauth.net/2/), [HTTPS](https://en.wikipedia.org/wiki/HTTPS), and many more security standards. This is a large part of the API and involves integration testing with front-end applications.
    
4. Database: understand [SQL](https://www.w3schools.com/sql/) commands, time complexities for queries, and database modeling. This is what the API will talk to after the user is authenticated. All data stored will be encrypted so this role overlaps with the security role.

5. Infrastructure using [Docker](https://www.docker.com/what-docker) and [Kuberneties](https://kubernetes.io/docs/concepts/overview/what-is-kubernetes/): use the two tools to deliver a final product and write user manuals. This involves documentation for how users can implement our solution and it will be shown in the poster presentation. May include a demo using [Amazon Web Services](https://aws.amazon.com/) or [Google Cloud Engine](https://cloud.google.com/compute/).
