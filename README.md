# Secure Calendaring Infrastructure
Open source solution for self hosted and infinitely scalable calendar, notes, and tasks.

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

# Architecture Overview
1. REST API lets clients authenticate using [OAuth](https://oauth.net/2/) (standard username and password setting) then lets them request their data from a server or update their data on a server. This API is hosted by the server and talks to a [SQL](https://en.wikipedia.org/wiki/SQL) server which accesses the user's data and decrypts it with their private key.
    * This REST API can be developed in nearly any language and there are numerous libraries that make this simple. [Here](http://cherrypy.org/) is an example of a Python library that lets you host a REST API in as little as 5 lines.
2. The API on the server responds to the client in a specific format, with the standard for calendar events being [iCalendar Spec RFC 5545](https://en.wikipedia.org/wiki/ICalendar). "Notes" and "Tasks" are just calendar events without due dates... many front end applications able to handle these differences.

The result of these two componenets is a service that will allow any user to access a secure calendar, notes, or tasks source from any app (google calendar, canvas calendar, apple, yahoo, thunderbird, evolution, outlook, etc). 

# What are we going to demonstrate in the presentation?
We are not developing a front-end, we will use an existing application (such as Outlook) to prove that our infrastructure works. A simple landing page such as [this](https://protonmail.com/) may be a nice visual to explain our security features and be the centerpiece of our final poster. We can make a simple diagram outlining the architecture of this project: a client talking to a server, the user authentication process, the server using its API to fetch user data, and the server responding back to the client with data. A general knowledge of networking and databases will get you through this entire project.

# Roles 
Of course there are overlapping responsibilities but these are the main components.
1. Simple web design (see [landing page example](https://protonmail.com/)). No hosting experience needed since GitHub does that for us.
   **Note your experience below**
    * Collin: Very little web experience but I can get around
    * Jessica:
    * Alex:
    * Michael:
    * Luke:
    
2. Security: someone that can use open source libraries to implement user authentication, HTTPS, etc. (see architecture section above)
 **Note your experience below**
    * Collin: I know many libraries for these tasks and I'm able implement it all
    * Jessica:
    * Alex:
    * Michael:
    * Luke:
    
3. Database: understand SQL commands and time complexities for these queries 
**Note your experience below**
    * Collin: some experience with SQL, might need some catching up, not a problem since it's not a complex table
    * Jessica:
    * Alex:
    * Michael:
    * Luke:
    
4. REST API development & networking: understand how servers work, how to make http calls and get a response. (see architecture section above)
**Note your experience below**
    * Collin: I've designed and developed several APIs over the past few years. Would be happy to lead this and teach about it.
    * Jessica:
    * Alex:
    * Michael:
    * Luke:
    
 5. Infrastructure using Docker and Kuberneties
 **Note your experience below**
    * Collin: Experienced using Docker in large organizations, very little experience with kuberneties but I can lead both and teach if needed.
    * Jessica:
    * Alex:
    * Michael:
    * Luke:
    
