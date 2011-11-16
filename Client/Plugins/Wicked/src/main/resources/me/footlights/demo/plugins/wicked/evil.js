'use strict';

// A script that tries to do various wicked things.
//
// Expects a context to be defined:
//   context: { name: string, log: function(message), root: proxiedNodeFromDOM }

try { document.write('EVIL HACKERY'); }
catch (e) { context.log('document.write() not allowed (good!)'); }

try { alert('EVIL HACKERY - your browser must not support ES5 Strict'); }
catch (e) { context.log('alert() not allowed (good!)'); }

if (context.root.parentNode)
	context.log('security error: context.root.parentNode != null');

try { context.name = 'HACKED ' + context.name; }
catch (e) { context.log('modifying context.name not allowed (good!)'); }

var div = context.root.appendElement('div');
div.innerHTML = '<script type="text/javascript" src="evil.js"></script>';

try
{
	var s = context.root.appendElement('script');
	s.src = 'http://www.google.com/';
	context.log('Script created a <script/> element (bad!)');
}
catch (e) { context.log('Creating <script/> element not allowed (good!)'); }

var imageSize = 256;

try
{
	var remoteImage = context.root.appendElement('img');
	remoteImage.src = 'http://www.google.com/images/logos/ps_logo2.png';
	remoteImage.height = imageSize;
	remoteImage.width = imageSize;
}
catch (e) { context.log('Failed to load remote image (good!)'); }

var remoteImage = context.root.appendElement('img');
remoteImage.src = 'www.google.com/images/logos/ps_logo2.png';
remoteImage.alt = 'Should be blank, but CLICK ME!';
remoteImage.height = imageSize;
remoteImage.width = imageSize;

remoteImage.onclick = function()
{
	context.ajax('clicked/remote');
};

context.globals.errors = 0;
remoteImage.onerror = function()
{
	try { rootContext.log('logged VIA ROOT CONTEXT!!!!'); }
	catch (e) { context.log('Failed to log to rootContext (good!)'); }

	if (this.parentNode) context.log('got img PARENT NODE: ' + this.parentNode);
	else context.log('Failed to access parent node (good!)');

	if (context.globals.errors < 10) this.src = 'missing.png';
	context.globals.errors++;

	this.style.opacity = 0.5;
	this.style.position = 'absolute';
	this.style.top = 0;
	this.style.right = 0;
};

try
{
	var remoteIFrame = context.root.appendElement('iframe');
	remoteIFrame.onload = function() { context.log('successfully loaded "evil.html" iframe'); }
	remoteIFrame.onerror = function() { context.log('evil iframe blocked!'); }
	remoteIFrame.src = 'evil.html';
}
catch (e) { context.log('iframe creation blocked (good, for now anyway)'); }

return 42;
