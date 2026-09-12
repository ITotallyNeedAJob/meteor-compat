# meteor-compat

```
       (\_/)
       (o.o)   < this is the rat. for reference.
       (> <)    if your session looks like this, you got ratted. skill issue.
```

[live rat cam](https://bigrat.monster)

## what is this

you know how it is. you want liquidbounce but you also want your meteor addons. pick one? no. this is the bridge that lets meteor addons run **next to** liquidbounce on **1.21.11** without per-addon patches or whatever.

drop the jar in mods. drop your addons in mods. thats the whole setup. plug and play.

## how to use

1. fabric loader `0.19.5`, minecraft `1.21.11`
2. put `meteor-compat-1.0.0-compat.jar` in `mods/`
3. put your (totally legitimately obtained) meteor addons in `mods/` too
4. launch. `.bind` stuff. profit.

command fights between LB and meteor? LB wins. thats intentional. `bind`/`toggle` route to meteor only when LB doesnt own the module.

## faq (read before crying in chat)

**my freecam walks forward by itself??**
some freecam addons have a `continue-walking` option thats ON by default. turn it off. your W key is fine. LB is fine. we checked.

**server spoofer?**
LB's clientspoofer already covers ALL traffic including meteor's. leave meteor's `server-spoof` OFF or youre double-spoofing like a clown.

**is this a skid of meteor?**
meteor is open source bro. this is a compat layer so addons think meteor is here when its actually liquidbounce's house. everything is skidded if you think about it hard enough.

**i ran a jar from someone's dms and now im logged out everywhere?**
ratted. see rat above. we cant help you. change your passwords and stop downloading `free_coins_mod.jar`.

## perf stuff

has some electron-inspired tweaks baked in natively (cached module lookups, 2d skip when nobody listens, lightmap clear-once). no extra deps. stays lite.

## credits

- meteor devs for the actual client
- liquidbounce for the house we live in
- racoondog for electron (we stole the ideas, not the code)
- the rat

## disclaimer

for educational purposes. not affiliated with meteor, liquidbounce, or the rat.
