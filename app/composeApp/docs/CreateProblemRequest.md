
# CreateProblemRequest

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **grade** | **kotlin.Int** | Numeric grade (converted to Fontainebleau on client) |  |
| **sectorName** | **kotlin.String** | Sector name (must match an existing sector folder) |  |
| **holdSequence** | **kotlin.collections.List&lt;kotlin.collections.List&lt;kotlin.Int&gt;&gt;** | Array of [index, type] where row/col are indices into the sector&#39;s holds array (0-indexed) and type is hold type (0&#x3D;Start, 1&#x3D;Foot, 2&#x3D;Normal, 3&#x3D;End) |  |
| **name** | **kotlin.String** | Problem name (defaults to \&quot;Problem {id}\&quot; if not provided) |  [optional] |
| **description** | **kotlin.String** |  |  [optional] |



