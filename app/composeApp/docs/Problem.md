
# Problem

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **id** | **kotlin.Int** |  |  |
| **author** | **kotlin.String** |  |  |
| **grade** | **kotlin.Int** | Numeric grade (converted to Fontainebleau on client) |  |
| **sectorName** | **kotlin.String** | Sector name |  |
| **holdSequence** | **kotlin.collections.List&lt;kotlin.collections.List&lt;kotlin.Int&gt;&gt;** | Array of [index, type] where index is the hold index of the sector&#39;s holds array (0-indexed) and type is hold type (0&#x3D;Start, 1&#x3D;Foot, 2&#x3D;Normal, 3&#x3D;End) |  |
| **name** | **kotlin.String** | Problem name (defaults to \&quot;Problem {id}\&quot; if not provided) |  [optional] |
| **description** | **kotlin.String** |  |  [optional] |
| **averageGrade** | **kotlin.Float** | Average of all user-submitted grades (null if no grades) |  [optional] |
| **averageStars** | **kotlin.Float** | Average of all user ratings (null if no grades) |  [optional] |
| **updatedAt** | **kotlin.String** |  |  [optional] |



