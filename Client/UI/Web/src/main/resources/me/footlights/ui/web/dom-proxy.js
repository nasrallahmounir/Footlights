/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Proxies access to DOM nodes.
//
// Provides a limited number of safe [TODO: are they?] DOM methods:
//   appendText(text)              returns a proxied Text node
//   appendElement(type)           returns a proxied Node
//

function proxy(node, context)
{
	var theProxy =
	{
		clear: function()
		{
			while (node.childNodes.length >= 1)
				node.removeChild(node.firstChild);
		},

		appendText: function(text)
		{
			var element = document.createTextNode(text);
			node.appendChild(element);
			return proxy(element, context);
		},

		appendElement: function(type)
		{
			var element = null;
			var subproxy = null;

			switch (type)
			{
				case 'iframe':
				case 'script':
					throw 'Sandboxed script attempted to create a ' + type + ' element';

				default:
					element = document.createElement(type);
					subproxy = proxy(element, context);
			}

			node.appendChild(element);
			return subproxy;
		},

		appendPlaceholder: function(name)
		{
			// We must create the span explicitly, rather than using appendElement(),
			// since we want to set the 'id' attribute (which is a privileged operation).
			var span = document.createElement('span');
			node.appendChild(span);

			// Now we can start using the unprivileged proxy for the <span/>.
			var subproxy = proxy(span, context);
			subproxy.class = 'placeholder';

			sandboxes['global'].ajax('fill_placeholder/' + name, function(s) { subproxy.appendText(s) });
			return subproxy;
		},

		get style() { return node.style; },

		set src(uri)        { node.src = '/static/' + context.name + '/' + uri; },

		set alt(text)       { node.alt = text; },
		set class(name)     { node.setAttribute("class", name); },
		set type(t)         { node.type = t; },
		set value(v)        { node.value = v; },
		set height(x)       { node.height = x; },
		set width(x)        { node.width = x; },


		// Event handlers must be proxied so that, when called, 'this' refers to the proxy object
		// and not the naked DOM object.
		proxy_code: function(js)
		{
			theProxy[js] = context.compile(js)(context.globals);
			return function() { theProxy[js](); }
		},

		set onclick(js)     { node.onclick      = theProxy.proxy_code(js); },
		set onerror(js)     { node.onerror      = theProxy.proxy_code(js); },
		set onload(js)      { node.onload       = theProxy.proxy_code(js); },
		set onmouseout(js)  { node.onmouseout   = theProxy.proxy_code(js); },
		set onmouseover(js) { node.onmouseover  = theProxy.proxy_code(js); },
	};

	return theProxy;
}

