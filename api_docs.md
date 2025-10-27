# API Documentation for AscendoTrainBoard

Ascendo TrainBoard allows clients to fetch custom climbing problems, alongside corresponding sector images and hold locations on the specified image.

**Base URL:** `/api/v1`

**Authentication:** Cookie-based sessions

**Response Format:** JSON

**Grading System:** Numeric grades (integers) - client converts to Fontainebleau scale

**Error Format:**
```json
{
  "error": "Error message description",
  "code": "ERROR_CODE"
}
```

---

## Authentication

### POST /auth/register
Register a new user account.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "username": "string",
  "is_admin": false
}
```
- Sets session cookie

**Errors:**
- 400 Bad Request: Invalid username or password
- 409 Conflict: Username already exists

---

### POST /auth/login
Authenticate user and obtain session cookie.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "string",
  "is_admin": false
}
```
- Sets session cookie

**Errors:**
- 401 Unauthorized: Invalid credentials

---

### POST /auth/logout
Logout user and clear session cookie.

**Authentication:** Required

**Response:** 204 No Content

---

## Sectors

Sectors are static and based on physical locations. They are identified by their folder names and cannot be created, updated, or deleted via the API. Sectors are discovered dynamically from the filesystem at server startup.

### GET /sectors
Retrieve a list of all climbing sectors.

**Response (200 OK):**
```json
[
  {
    "name": "sector-a"
  },
  {
    "name": "sector-b"
  }
]
```

---

### GET /sectors/{name}
Retrieve details of a specific climbing sector by name.

**Response (200 OK):**
```json
{
  "name": "sector-a",
  "holds": [
    [100, 150, 120, 170],
    [200, 175, 215, 190],
    [150, 300, 165, 320]
  ]
}
```

**Fields:**
- `name`: Sector name (matches the folder name)
- `holds`: Array of hold bounding boxes as `[start_x, start_y, end_x, end_y]` (0-indexed pixel coordinates)

**Errors:**
- 404 Not Found: Sector does not exist

---

### GET /sectors/{name}/image
Retrieve the sector wall image.

**Response:** Image file (JPEG/PNG)

**Content-Type:** `image/jpeg` or `image/png`

**Errors:**
- 404 Not Found: Sector does not exist

---

## Climbing Problems

### GET /problems
Retrieve a list of climbing problems with optional filtering and pagination.

**Query Parameters:**
- `sector` (optional): Filter by sector name
- `grade` (optional): Filter by grade
- `author` (optional): Filter by author username
- `page` (optional): Page number, default 1
- `per_page` (optional): Items per page, default 20

**Response (200 OK):**
```json
{
  "problems": [
    {
      "id": 1,
      "name": "The Crimp Master",
      "description": "A technical problem focusing on crimps",
      "author": "john_doe",
      "grade": 6,
      "sector_id": "sector-a",
      "average_grade": 6.5,
      "average_stars": 4.5,
      "created_at": "2025-01-15T10:30:00Z",
      "updated_at": "2025-01-15T10:30:00Z"
    }
  ],
  "total": 42,
  "page": 1,
  "per_page": 20
}
```

**Fields:**
- `description`: Optional text description of the problem
- `average_grade`: Calculated average of all user-submitted grades (null if no grades yet)
- `average_stars`: Calculated average of all user ratings (null if no grades yet)

**Note:** `hold_sequence` is not included in the list endpoint to reduce traffic. Fetch individual problems to get hold sequences.

---

### GET /problems/{id}
Retrieve details of a specific climbing problem by ID.

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "The Crimp Master",
  "description": "A technical problem focusing on crimps",
  "author": "john_doe",
  "grade": 6,
  "sector_id": "sector-a",
  "hold_sequence": [[0, 1], [1, 2], [2, 3]],
  "average_grade": 6.5,
  "average_stars": 4.5,
  "created_at": "2025-01-15T10:30:00Z",
  "updated_at": "2025-01-15T10:30:00Z"
}
```

**Fields:**
- `hold_sequence`: Array of `[row, col]` indices into the sector's holds array (0-indexed)
- `average_grade`: Calculated average of all user-submitted grades (null if no grades yet)
- `average_stars`: Calculated average of all user ratings (null if no grades yet)

**Errors:**
- 404 Not Found: Problem does not exist

---

### POST /problems
Create a new climbing problem.

**Authentication:** Required

**Request Body:**
```json
{
  "name": "string (optional)",
  "description": "string (optional)",
  "grade": 1,
  "sector_id": "sector-a",
  "hold_sequence": [[0, 1], [1, 2]]
}
```

**Required Fields:**
- `grade`: Problem difficulty grade (integer)
- `sector_id`: Sector name (string, must match an existing sector folder)
- `hold_sequence`: Array of hold indices `[row, col]`

**Optional Fields:**
- `name`: Problem name (if not provided, defaults to "Problem {id}")
- `description`: Text description

**Note:** `author` is automatically set to the authenticated user

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "The Crimp Master",
  "description": "A technical problem focusing on crimps",
  "author": "john_doe",
  "grade": 6,
  "sector_id": "sector-a",
  "hold_sequence": [[0, 1], [1, 2], [2, 3]],
  "average_grade": null,
  "average_stars": null,
  "created_at": "2025-01-15T10:30:00Z",
  "updated_at": "2025-01-15T10:30:00Z"
}
```

**Errors:**
- 401 Unauthorized: Not authenticated
- 400 Bad Request: Invalid data
- 404 Not Found: Sector does not exist

---

### PUT /problems/{id}
Update an existing climbing problem.

**Authentication:** Required (must be problem owner or admin)

**Request Body (all fields optional):**
```json
{
  "name": "string",
  "description": "string",
  "grade": 1,
  "hold_sequence": [[0, 1], [1, 2]]
}
```

**Note:** `sector_id` and `author` cannot be changed

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Updated Name",
  "description": "Updated description",
  "author": "john_doe",
  "grade": 7,
  "sector_id": "sector-a",
  "hold_sequence": [[0, 1], [1, 2], [2, 4]],
  "average_grade": 6.5,
  "average_stars": 4.5,
  "created_at": "2025-01-15T10:30:00Z",
  "updated_at": "2025-01-16T14:20:00Z"
}
```

**Errors:**
- 401 Unauthorized: Not authenticated
- 403 Forbidden: Not the problem owner or admin
- 404 Not Found: Problem does not exist
- 400 Bad Request: Invalid data

---

### DELETE /problems/{id}
Delete a climbing problem.

**Authentication:** Required (must be problem owner or admin)

**Response:** 204 No Content

**Errors:**
- 401 Unauthorized: Not authenticated
- 403 Forbidden: Not the problem owner or admin
- 404 Not Found: Problem does not exist

---

## Problem Grades/Ratings

Users can submit their own grade suggestions and ratings for problems.

### GET /problems/{id}/grades
Retrieve all grades/ratings for a specific problem.

**Response (200 OK):**
```json
{
  "problem_id": 1,
  "grades": [
    {
      "username": "john_doe",
      "grade": 6,
      "stars": 4,
      "created_at": "2025-01-15T10:30:00Z"
    },
    {
      "username": "jane_smith",
      "grade": 7,
      "stars": 5,
      "created_at": "2025-01-16T12:00:00Z"
    }
  ],
  "average_grade": 6.5,
  "average_stars": 4.5
}
```

**Fields:**
- `grade`: User's suggested grade for the problem (integer)
- `stars`: User's rating (1-5)
- `average_grade`: Calculated average of all submitted grades
- `average_stars`: Calculated average of all star ratings

**Note:** Each user can only submit one grade per problem. Submitting again updates their existing grade.

**Errors:**
- 404 Not Found: Problem does not exist

---

### POST /problems/{id}/grades
Submit or update a grade/rating for a problem.

**Authentication:** Required

**Request Body:**
```json
{
  "grade": 6,
  "stars": 4
}
```

**Fields:**
- `grade`: Suggested grade (integer, converted to Fontainebleau on client)
- `stars`: Rating (1-5)

**Note:** If the user has already graded this problem, their existing grade is updated. Otherwise, a new grade is created. Users can only have one grade per problem.

**Response (201 Created or 200 OK):**
```json
{
  "username": "john_doe",
  "grade": 6,
  "stars": 4,
  "created_at": "2025-01-15T10:30:00Z"
}
```

**Errors:**
- 401 Unauthorized: Not authenticated
- 404 Not Found: Problem does not exist
- 400 Bad Request: Invalid grade or stars value

---

## Admin

**Note:** Admin users have full permissions (similar to Unix root) and can perform all operations including editing/deleting any user's problems.

### GET /admin/users
Retrieve a list of all users.

**Authentication:** Required (admin only)

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "username": "john_doe",
    "is_admin": false,
    "created_at": "2025-01-15T10:30:00Z"
  },
  {
    "id": 2,
    "username": "admin_user",
    "is_admin": true,
    "created_at": "2025-01-10T08:00:00Z"
  }
]
```

**Errors:**
- 401 Unauthorized: Not authenticated
- 403 Forbidden: Not an admin

---

### GET /admin/users/{id}
Retrieve details of a specific user by ID.

**Authentication:** Required (admin only)

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "john_doe",
  "is_admin": false,
  "created_at": "2025-01-15T10:30:00Z"
}
```

**Errors:**
- 401 Unauthorized: Not authenticated
- 403 Forbidden: Not an admin
- 404 Not Found: User does not exist

---

### PUT /admin/users/{id}
Update user details.

**Authentication:** Required (admin only)

**Request Body (all fields optional):**
```json
{
  "username": "new_username",
  "password": "new_password",
  "is_admin": true
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "new_username",
  "is_admin": true,
  "created_at": "2025-01-15T10:30:00Z"
}
```

**Errors:**
- 401 Unauthorized: Not authenticated
- 403 Forbidden: Not an admin
- 404 Not Found: User does not exist
- 409 Conflict: Username already exists

---

### DELETE /admin/users/{id}
Delete a user account.

**Authentication:** Required (admin only)

**Response:** 204 No Content

**Errors:**
- 401 Unauthorized: Not authenticated
- 403 Forbidden: Not an admin
- 404 Not Found: User does not exist

---

## Common HTTP Status Codes

- **200 OK**: Request succeeded
- **201 Created**: Resource created successfully
- **204 No Content**: Request succeeded with no response body
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Authenticated but not authorized
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource conflict (e.g., duplicate username)
- **500 Internal Server Error**: Server error