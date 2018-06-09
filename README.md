# ammonite-scripts
Collection of useful [Ammonite](http://ammonite.io) scripts, mainly for personal purpose, but the goal is also to test the possibility to replace *bash* with *Scala*-based scripts for general purpose.


## mat35mm ([ => source code](https://github.com/vaclavsvejcar/ammonite-scripts/blob/master/scripts/mat35mm.sc))
This script fetches all upcoming 35mm film screenings from the Prague [Kino MAT](http://mat.cz) (MAT cinema).

*Example output*:
```
$ amm mat35mm.sc
] Fetching list of all future screenings: http://www.mat.cz/matclub/cz/kino/mesicni-program
] Looking for 35mm screenings 100% │██████████████████████████████████████████│ 39/39 (0:00:04 / 0:00:00) finished

] Following 4 of 39 upcoming screenings are in 35mm:
   1/ Old Shatterhand (http://www.mat.cz/matclub/cz/kino/mesicni-program?movie-id=4964_old-shatterhand)
        #1: pátek 15. 6. od 18.15
   2/ V říši Stříbrného lva (http://www.mat.cz/matclub/cz/kino/mesicni-program?movie-id=5370_v-risi-stribrneho-lva)
        #1: pátek 22. 6. od 18.45
   3/ Poklad na Stříbrném jezeře (http://www.mat.cz/matclub/cz/kino/mesicni-program?movie-id=4588_poklad-na-stribrnem-jezere)
        #1: pátek 29. 6. od 16.30
   4/ Vinnetou - Poslední výstřel (http://www.mat.cz/matclub/cz/kino/mesicni-program?movie-id=4963_vinnetou-posledni-vystrel)
        #1: pátek 29. 6. od 18.30

```
