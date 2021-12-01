# Novelty-Search-Using-GAs
A maze finder that operates through genetic algorithms that have their fitness function determined by their novelty metric. For this specific project, these inputs are funneled into a Stringflow library. The library allows an emulator to use these inputs.

Here's a gif of it in action, discovering the map tile by tile. Red tiles are newly discovered and blue ones are old. 

The main power behind this is the ability to maintain past structure despite mutations and the ability to reinvigorate the population through the addition of previous successes that might now be more novel than before.

![save-11](https://user-images.githubusercontent.com/20337549/140167383-dd897a6a-deac-4639-8196-2c954194d516.gif)

The gif shows the exploration of the agents overtime. Each tile represents a new path to a tile without hitting an undetectable random encounter by manipulating their path through the timing of their inputs. Red tiles are new and blue tiles are old.
