# cljs-analyzer

A boilerplate/playground for writing audio visualisations with Web Audio API, ClojureScript and Figwheel, but also a playground for writing reloadable code and exploring it with those technologies.

Keep in mind the API is still in flux and there's plenty of bugs (hence no real release yet).

## Overview

The aim is to have a reusable, interactive setup for creating audio visualisations in the browser, driven by REPL interactions.

The workflow should be as follows:

1. Create a config
2. Write your render function
3. Play your track
4. Modify your render function
5. Rinse and repeat "4" until done

## Getting started

_Note: Use the below command with `rlwrap` (or similar readline wrapper) which will provide repl history and
generally improve your REPL experience_

To get started run:

    lein figwheel

This will open your browser at [localhost:3449](http://localhost:3449/). After the compilation process is complete, you will
get a browser connected REPL. An easy way to test it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To start a visualisation you will need 3 things: a config, a state atom and a render function.

### Config

- `state`

This should just be a simple atom, containing a map and defined with defonce. It will hold the instances
of AudioContext and CanvasContext.

- `render`

A function that will get the dereferenced state atom as it's first argument and the config as it's second. It will be passed to `requestAnimationFrame` and is responsible for drawing your visualisation.

- `config`

The config is map that should have the following keys:

-- `:data` - a `Uint8Array` that will hold the [analyser node](http://devdocs.io/dom/analysernode) data
-- `:render` - the function described above
-- `:width` - the width of the canvas element
-- `:height` - the height of the canvas element
-- `:track` - the URL of the track to be used
-- `:background` - the initial background color of the visualisation

### API

The `cljs-analyzer.core` namespace provides the following methods:

-- `setup` - this takes the config and state as arguments and sets up the visualisation
-- `teardown` - this takes the config and state as arguments and teardowns the whole visualisation (clearing the html and closing the contexts)
-- `reset` - this takes the config and state as arguments and calls first `teardown` and then `setup`
-- `reload` - pass this function to Fighwheels `on-js-reload` and it will reload only your render function, but not your config


## License

Copyright © 2017 Dominik Piatek

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
