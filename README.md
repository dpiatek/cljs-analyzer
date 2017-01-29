# cljs-analyzer

A boilerplate for writing audio visualisations:

## Overview

The aim of this project is to have a reusuable, interactive setup for creating
audio visualisations in the browser, using clojurescipt, figwheel and repl
centered development, where the flow is as follows:

1. Create a config
2. Write your render function
3. Use figwheel and the repl to interactively develop your visualisation

### Configuration

The config must be a map with the following keys:
- `:freq-data` - a `Uint8Array` that will hold the [analyser node](http://devdocs.io/dom/analysernode) data
- `:root` - this must be a clojure atom that will hold the drawing and audio contexts
- `:frame` - a function that will get the dereferenced atom containing contexts and this full config and will be responsibile
for re-drawing things on the screen
- `:width` - the width of the canvas element
- `:height` - the height of the canvas element
- `:track` - the URL of the track to be used

### API:
- `(setup config)` - creates the HTML and sets up the contexts. Takes the `config`.
- `(teardown (deref (:root config)))` - undoes the setup, removes the HTML and closes the contexts. Used with figwheel when reloading js.
- `animation-frame-id` - use this reference to `set!` the id of `requestAnimationFrame` so it can be canceled
when the pause and stop buttons are pressed
```
(set! c/animation-frame-id (.requestAnimationFrame js/window (partial frame root config))))
```

## Setup

_Note: Use the below command with `rlwrap` which will provide repl history and
generally improve your dev experience_

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## License

Copyright © 2017 Dominik Piatek

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
