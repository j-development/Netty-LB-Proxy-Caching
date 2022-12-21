# Loadbalancing service with caching in Netty java framework

## Skalbara Tjänster

Loadbalancer endpoint: localhost:5000/product/all
Nodes startingport: 5071

When the node cluster reaches above 1.0 average requests, a new node is deployed.
When requests are below 0.5 average requests. A counter starts and after 
about 60 seconds with low requests a node is deprovisioned

Caching is done with Google Guava cachebuilder. First time when the request is made
it goes through and is put into the cache. When same request goes through a second time
it will be picked up and serviced without going through to the nodes. Also,
when a second client connects the standard overhead is applied with activating a channel and so on,
but will actually use the cache to service the request that is already cached from the first client
and some overhead computing is spared.