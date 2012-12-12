=================================
Zazl Dynamic JavaScript Optimizer
=================================

The Zazl Javascript Optimizer enables developers to produce optimized JavaScript environments for their AMD based and Dojo syncloader based applications 
without having to depend on static build/optimizer tools.

Downloads
=========
Downloads of the Zazl Optimizer and samples can be found here - http://www.zazl.org/?page_id=5

General Optimizer Features
==========================

* Supports both validation based caching and expiration based caching.
* Supports compression of resulting JavaScript response delivered to browser.
* Easy to setup within JEE based Web Applications.

AMD Optimizer Features
======================

* Includes its own AMD Compliant Loader (It passes all the current AMD JS Tests)
* Can optimize Dojo 1.6.x and 1.7.x based applications.
* Optimizes locale messages based on the browsers locale when the dojo/i18n plugin is used.
* Optimizes text data when the dojo/text plugin is used.
* Can Optimize jQuery 1.7.x based applications that use AMD.
* Provides a JEE HTML Filter that will parse HTML and insert required javascript tags.
* Includes a Standalone Jetty Zazl Optimizer Server
* Includes a NodeJS Zazl Optimizer

Dojo Syncloader Optimizer Features
==================================

* Can optimize up to Dojo 1.5.x based applications.
* Optimizes locale messages based on the browsers locale when the dojo i18n support is used.
* Optimizes dojo widget templates.

Getting Started
===============

See the Getting Started Pages for more details :

:Getting Started with the AMD Optimizer: https://github.com/zazl/optimizer/wiki/Getting-Started-with-the-AMD-Optimizer
:Getting Started with the Dojo Syncloader Optimizer: https://github.com/zazl/optimizer/wiki/Getting-Started-with-the-Dojo-Syncloader-Optimizer
:Getting Started via OSGi/Eclipse: https://github.com/zazl/optimizer-osgi-boilerplate

Browser Support
===============

The Zazl AMD Loader should work in the following browsers :

* Firefox
* Safari 5+
* Chrome
* IE9+
* Mobile Safari on iOS 4+
* Android Browser 1.6+

Setting up a Development Environment
====================================

* Follow the development environment steps described in the README on https://github.com/zazl/serverutils.
* Clone the Optimizer git repo (git clone https://github.com/zazl/optimizer.git) from within the "zazldev/workspace" directory.
* Import the projects found in the "zazldev/workspace/optimizer" directory via "File->Import->General->Existing Projects into Workspace".

License
=======

Copyright (c) The Dojo Foundation 2011. All Rights Reserved.

