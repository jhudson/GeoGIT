.. _risk_list:

Risk List
#########


This document records the major project risks, and plans to control them. For each risk the plan should include:

* Mitigation plan: Measures to carry out now, to reduce the likelihood and/or impact of the risk. Alternatively, we can decide to accept the risk.

* Indicator: A sign to monitor in order to determine if the risk is beginning to have an impact on the project.

* Contingency plan: What we plan to do if the risk does arise.

The severity of a risk is its likelihood multiplied by its impact. Risks are classified as `minor` if they have low likelihood, `negligible` impact, or `medium` likelihood and marginal impact.


Major risks
***********

.. warning:: The following risks are just briefly outlined. They need to be discussed/elaborated

* Requirements are only partly known at project start. We should be good for the first couple iterations though, and overall it feels like we all have a pretty good idea of what we want to build. Just make sure we make a couple validation rounds with the customer.
* List of :ref:`participants` is not finished

* We might want to consider using `SHA-256 <http://en.wikipedia.org/wiki/SHA-2>`__ instead of SHA1. Likelihood of collisions seems to be enormously low with SHA1 anyway, but if this project is to grow and actually handle billions of entres (think OSM history) and SHA-2 performance is not a blocker, we might consider it sooner rather than later.

* **Spatial Indexing** is gonna be a huge requirement and is something that hasn't been evaluated yet. Especially if its (most likely) deemed necessary to use some kind of "multi-dimensional" indexing. Think dataset snapshot per commit/branch. It wouldn't be practical to have a separate spatial index for each combination.

* Supporting **Android** would require either making GeoTools working on the android platform (unlikely with a "reasonable" level of effort), or making it "optional" and find  replacement libraries for android (at a first glance it seems it should suffice with anything that provides "referencing" and coordinate transformation services (proj4j?), but since the evil is in the details...

* Make sure to allocate enough time next iteration to define what the IPC/RPC needs are and the approach to take. Evaluate alternatives (hessian? REST? protobuf? etc).

* A second (third?) round to defining the "canonical binary representation of a feature/feature type" may be needed. Plan and schedule accordingly. Rationale being that we may not want to be tied to potential binary format changes when upgrading the library used to create the binary representations (currently Hessian), and screw up the hashing (since two identical objects wouldn't result in the same hash anymore).