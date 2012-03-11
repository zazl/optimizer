=================================
Zazl Dynamic JavaScript Optimizer
=================================

The Zazl Javascript Optimizer enables developers to produce optimized JavaScript environments for their AMD based and Dojo syncloader based applications 
without having to depend on static build/optimizer tools.

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
* Optimizes text data when the dojo/text.
* Can Optimize jQuery 1.7.x based applications that use AMD.
* Provides a JEE HTML Filter that will parse HTML and insert required javascript tags.

Dojo Syncloader Optimizer Features
==================================

* Can optimize up to Dojo 1.5.x based applications.
* Optimizes locale messages based on the browsers locale when the dojo i18n support is used.
* Optimizes dojo widget templates.

Getting Started
===============

See the Getting Started Wiki Pages for more details :

:Getting Started with the AMD Optimizer: https://github.com/zazl/optimizer/wiki/Getting-Started-with-the-AMD-Optimizer
:Getting Started with the Dojo Syncloader Optimizer: https://github.com/zazl/optimizer/wiki/Getting-Started-with-the-Dojo-Syncloader-Optimizer

Browser Support
===============

The Zazl AMD Loader should work in the following browsers :

* Firefox
* Safari 5+
* Chrome
* IE9+
* Mobile Safari on iOS 4+
* Android Browser 1.6+

License
=======

Copyright (c) The Dojo Foundation 2011. All Rights Reserved.

