## Fork of JVBench Benchmark Suite

This is a playground from a practitioner's perspective.

## What's the problem with blogs talking about the JDK vector API?

Most blogs either use a 4 line example crawling through an array and doing some stuff that is just too simple
to be of practical use or they provide bigger examples that use floats.
What's the problem with using floats you might ask? Well for more complicated calculations you will really need
double precision, so floats won't work. Otherwise the error propagated gets too big.
There are cases of course where floats make perfect sense, for example computing the price of vanilla options
but there the calculation is not the bottleneck. More time is usually wasted on getting the data (e.g. parsing xml) and
setting up internal structures than pricing (about 5x more!). Looking at the BS example, I see that spots, vols, maturity
and rates are inputs. That's also not a realistic assumption. You have one spot price for an asset, one fixed rate and
one vol as function of strike and maturity. You might want to price for a given asset, multiple strikes and maturities.
But you only have different spots or rates if you want to compute Greeks (derivatives).
This example might just benefit from vectorized loading of data and not so
much improved computation efficiency.

## Lessons learned for using Scala + vector api

- The `species` should be `constant`, meaning don't pass it as an input to a method (unless it's inlined).
 Nobody mentions this explicitly but you'll find that all Java examples keep the `species` in a static variable.  
- Never ever pass `DoubleVector` to a method that is not inlined by the Scala compiler. Otherwise the jvm will allocate 
 an object.
- Activate the inlining in the Scala optimizer and put `@inline` annotations. 
 Be aware that variables might _disappear_ during debugging, so you might have to turn it off for that purpose.

## Running

Use intellij + jvm plugin, don't forget to add. `--add-modules jdk.incubator.vector`

## Results

```
Benchmark                          Mode  Cnt  Score   Error  Units
BlackscholesBenchmark.autoVec        ss   50  1.283 ± 0.017   s/op
BlackscholesBenchmark.explicitVec    ss   50  7.773 ± 0.257   s/op
BlackscholesBenchmark.fullVec        ss   50  7.981 ± 0.341   s/op
BlackscholesBenchmark.serial         ss   50  1.273 ± 0.013   s/op
```

## References

https://alexklibisz.com/2023/02/25/accelerating-vector-operations-jvm-jdk-incubator-vector-project-panama