## 2024-05-24 - Memoize list operations in Compose
**Learning:** Found multiple places where collections are chunked inside Compose's `LazyRow` `items` builder, such as `items(shelf.items.chunked(RECENT_TRACKS_PER_COLUMN))` in HomeScreen.kt and `items(top.chunked(SONGS_PER_COLUMN))` in DetailScreen.kt, and `section.items.chunked(2)` in ExploreScreen.kt. This creates a new List of Lists on every recomposition.
**Action:** Use `remember` to memoize list transformations like `chunked` to prevent unnecessary allocations during recompositions.
