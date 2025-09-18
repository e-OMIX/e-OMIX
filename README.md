# e-OMIX Application

e-OMIX is a platform of analysis and exploration of omics data.

🚀 **Technologies Used**

* Frontend: Angular 20.2.2, Angular CLI
* Backend: Java Spring Boot
* Database: CouchDB
* Object Storage: MinIO
* Build Tools: npm 11.0.0, Docker
* Version Management: nvm 0.40.3, Git CLI

⚠️ **Platform Compatibility Note**

This version of e-OMIX is designed to work exclusively on **Linux and macOS** systems. Windows support is currently in development and will be available in a future release.

📋 **Prerequisites**

Before you begin, ensure you have the following software installed on your machine:

1. **Git CLI**: For version control and cloning the repository.
   * Linux: `sudo apt-get install git`
   * macOS: `brew install git`

2. **Docker**: For running MinIO and other containerized services.
   * Linux: `sudo apt-get install docker.io` or follow [Docker installation guide](https://docs.docker.com/engine/install/)
   * macOS: Download from [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)
   * Make sure Docker is running before proceeding: `sudo systemctl start docker` (Linux) or via Docker Desktop (macOS)

3. **Java Development Kit (JDK) 23**: For running the Spring Boot backend.
   * Linux: `sudo apt install openjdk-23-jdk`
   * macOS: `brew install openjdk@23`
   * Verify installation: `java --version`

4. **Node.js 22.19.0** (Recommended via NVM): For running the Angular frontend and npm.
   * Install NVM:
     ```bash
     curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
     ```
   * Install and use Node.js:
     ```bash
     nvm install 22.19.0
     nvm use 22.19.0
     ```

5. **npm 11.0.0**: Usually comes bundled with Node.js. Verify version: `npm --version`

6. **Angular CLI**: To scaffold and serve the Angular project.
   ```bash
   npm install -g @angular/cli
   ```

7. **CouchDB 3.0+**: The document-based database for this project.
   * Linux (Ubuntu/Debian):
     ```bash
     sudo apt update
     sudo apt install couchdb
     ```
   * macOS:
     ```bash
     brew install couchdb
     brew services start couchdb
     ```

8. **Python 3.11**: Required for running Python scripts.
   * Linux:
     ```bash
     sudo apt update
     sudo apt install python3.11 python3-pip
     ```
   * macOS:
     ```bash
     brew install python@3.11
     ```
   * Verify installation: `python3.11 --version`

⚙️ **MinIO Installation and Setup**

1. **Run MinIO with specific release**:
   ```bash
   docker run -p 9000:9000 -p 9001:9001 --name minio \
   -v ~/minio/data:/data \
   -e "MINIO_ROOT_USER=user" \
   -e "MINIO_ROOT_PASSWORD=password" \
   quay.io/minio/minio:RELEASE.2025-02-28T09-55-16Z server /data --console-address ":9001"
   ```

   ⚠️ **Important**: Change the default credentials (`MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD`) to secure values in the command above before running it.

2. **Access MinIO Console**:
   * Open your browser and go to `http://localhost:9001`
   * Login with the credentials you set in the docker command

3. **Create Access Key**:
   * In the MinIO Console, go to **Access Keys** → **Create Access Key**
   * Click **Create** and **make sure to copy both Access Key and Secret Key immediately** as they won't be shown again
   * Store these credentials securely

⚙️ **CouchDB Setup**

After installing CouchDB, you need to configure it and create the necessary databases with their design documents.

1. **Access the Admin Console**: Open your browser and go to `http://localhost:5984/_utils/`.
2. **Complete Setup** (First Time Only):
   * Follow the single-node setup wizard.
   * Create an admin user and a strong password. Remember these credentials.
   * Do not enable "Admin Party" (anonymous admin access) for a real project.
3. **Create the Databases**: In the CouchDB Admin Console (Fauxton), click on "Create Database" and create the following three databases:
   * `eomix`
   * `experiment`
   * `attachment`

4. **Add Design Documents**:

   **Option 2: Manual setup using the Fauxton Web Interface**:
   - Navigate to each database
   - Click "New Document"
   - Copy and paste the content from the design document (see  below)
   - Save the document with the exact ID specified in the file (e.g., `_design/FileUploadEntity`)
For the eomix database:
**FileUploadEntity**
```json
 {
  "_id": "_design/FileUploadEntity",
  "views": {
    "by_filename": {
      "map": "function (doc) {\n  if (doc.meta.filename) { emit(doc.meta.filename, doc);}\n}"
    },
    "file_metadata": {
      "map": "function (doc) {\n   var key = doc.meta.filename;\n      var value = {\n        filename: doc.meta.filename,\n        species: doc.sourceColumns.standardized_species ,\n        sequenceType:doc.sourceColumns.sequenceType,\n        cellularResolution:doc.sourceColumns.cellularResolution,\n        count: 1,\n        disorder: [doc.sourceColumns.disorder],\n        protocol: doc.sourceColumns.protocol,\n        organ: [doc.sourceColumns.organ]\n      };\n      emit(key, value);\n}",
      "reduce": "function (keys, values, rereduce) {\n  var result = {\n     filename: values[0].filename,\n     species: values[0].species,\n     sequenceType:values[0].sequenceType,\n     cellularResolution:values[0].cellularResolution,\n    protocol:values[0].protocol,\n    count: 0,disorder: [], organ: []};\n    values.forEach(function(v) {\n      result.count += v.count;\n      result.disorder =result.disorder.concat( v.disorder);\n      result.organ = result.organ.concat(v.organ);\n    });\n    // Remove duplicates\n    result.disorder= [...new Set(result.disorder)];\n    result.organ = [...new Set(result.organ)];\n    return result;\n}"
    },
    "get_samples_by_filename": {
      "reduce": "_count",
      "map": "function (doc) {\n emit(doc.meta.filename, {\n      filename: doc.meta.filename,\n        sample_id: doc.sourceColumns.sample_id\n    });\n}"
    }
  },
  "language": "javascript"
} ```
```
 **MetadataFileUploadEntity**

```json 
{
  "_id": "_design/MetadataFileUploadEntity",
  "views": {
    "by_filename": {
      "map": "function (doc) {\n if (doc.meta.filename) { emit(doc.meta.filename, doc);}\n}"
    },
    "file_metadata": {
      "map": "function (doc) {\n   var key = doc.meta.filename;\n      var value = {\n        filename: doc.meta.filename,\n        species: doc.sourceColumns.standardized_species ,\n        sequenceType:doc.sourceColumns.sequenceType,\n        cellularResolution:doc.sourceColumns.cellularResolution,\n        count: 1,\n        disorder: [doc.sourceColumns.disorder],\n        protocol: doc.sourceColumns.protocol,\n        organ: [doc.sourceColumns.organ]\n      };\n      emit(key, value);\n}",
      "reduce": "function (keys, values, rereduce) {\n  var result = {\n     filename: values[0].filename,\n     species: values[0].species,\n     sequenceType:values[0].sequenceType,\n     cellularResolution:values[0].cellularResolution,\n    protocol:values[0].protocol,\n    count: 0,disorder: [], organ: []};\n    values.forEach(function(v) {\n      result.count += v.count;\n      result.disorder =result.disorder.concat( v.disorder);\n      result.organ = result.organ.concat(v.organ);\n    });\n    // Remove duplicates\n    result.disorder= [...new Set(result.disorder)];\n    result.organ = [...new Set(result.organ)];\n    return result;\n}"
    },
    "get_samples_by_filename": {
      "reduce": "_count",
      "map": "function (doc) {\n emit(doc.meta.filename, {\n      filename: doc.meta.filename,\n        sample_id: doc.sourceColumns.sample_id\n    });\n}"
    }
  },
  "language": "javascript"
}
```
For the experiment database:
 **experiments**

```json
{
  "_id": "_design/experiments",
  "views": {
    "by_experimentNameAndType": {
      "map": "function (doc) {\n\t  if (doc.experimentType) { \n\t    emit(doc.experimentType, { \n\t      experimentName: doc.experimentName,  \n\t      experimentType: doc.experimentType,  \n\t      status: doc.status,  \n\t      metadataFileName: doc.metadataFileName, \n\t      omicsModality: doc.omicsModality,\n        cellularResolution: doc.cellularResolution,\n        createdAt: doc.createdAt, }); }\n}"
    },
    "by_metadataFileName": {
      "map": "function (doc) {\n   if (doc.metadataFileName) { \n     emit(doc.metadataFileName, {  \n       experimentName: doc.experimentName,  \n       experimentType: doc.experimentType,  \n       status: doc.status,  \n       metadataFileName: doc.metadataFileName,\n      omicsModality: doc.omicsModality,\n      cellularResolution: doc.cellularResolution,\n      createdAt: doc.createdAt,}); }\n}"
    }
  },
  "language": "javascript"
}
```

For the attachment database:
 **csv_docs**

```json
{
  "_id": "_design/csv_docs",
  "views": {
    "by_filename": {
      "map": "function (doc) {\nif (doc._attachments) { \n          for (var filename in doc._attachments) { \n              emit(filename, doc._id); \n          } \n      }\n}"
    },
    "by_name": {
      "map": "function(doc) { if (doc._attachments) { for (var name in doc._attachments) { emit(name, doc._attachments[name]); } } }"
    }
  },
  "language": "javascript"
}
```

  
5. **Verify Design Documents**:
   After adding, you can verify your views work by querying them:
   ```
   http://localhost:5984/eomix/_design/MetadataFileUploadEntity/_view/file_metadata
   ```

⬇️ **Project Installation & Setup**

Follow these steps to get your development environment running:

1. **Clone the repository**:
   ```bash
   git clone git@github.com:e-OMIX/e-omix_app.git
   cd e-omix_app
   ```

2. **Backend (Spring Boot) Setup**:
   ```bash
   # Navigate to the backend directory
   cd backend

   # Install dependencies (using Maven)
   ./mvnw clean install
   ```

3. **Configure Database Connection**:
   * Locate the `application.properties` file in your Spring Boot project, typically at `backend/src/main/resources/application.properties`.
   * Update the CouchDB connection settings with the credentials you created during the CouchDB setup:
     ```
     # CouchDB Configuration
     couchdb.url=http://localhost:5984
     couchdb.username=YOUR_COUCHDB_USERNAME
     couchdb.password=YOUR_COUCHDB_PASSWORD
     ```
   * Replace with your actual CouchDB credentials.

4. **Frontend (Angular) Setup**:
   ```bash
   # Navigate to the frontend directory
   cd frontend

   # Install all project dependencies
   npm install
   ```
   
🔧 **Configuration Updates**

Before running the application, update the credentials in the code:

1. **Search for the following placeholders throughout the entire project**:
   * `YOUR_MINIO_SECRET_KEY` - Replace with your MinIO Secret Key
   * `YOUR_MINIO_ACCESS_KEY` - Replace with your MinIO Access Key
   * `YOUR_COUCHDB_USERNAME` - Replace with your CouchDB username
   * `YOUR_COUCHDB_PASSWORD` - Replace with your CouchDB password

3. **Update these credentials in**:
   * Java backend code
   * R scripts
   * Python scripts
     -> you can you the option "Find in Files..." in you IDE.

🐍 **Python Setup**

1. **Install required Python packages**:
   ```bash
   pip3 install couchdb
   pip3 install minio
   ```

🏗️ **Docker Image Building**

Before launching the application, build the required Docker images:

1. **Navigate to the resources directory**:
   ```bash
   cd sbackend/src/main/resources
   ```

2. **Build post-processing image** (this may take a while):
   ```bash
   docker build -f scRNA-seq/post-processing/Dockerfile -t post-processing .
   ```

3. **Build visualization image**:
   ```bash
   docker build -f scRNA-seq/visualization/Dockerfile -t visualization .
   ```

⚠️ **Important**: Ensure Docker is running throughout this process and during application execution.

🏃‍♂️ **Running the Application**

You need to run both the backend server and the frontend development server. Ensure all services are running before starting.

**Starting Services**:

1. **Docker**: Make sure Docker is running
   ```bash
   # Linux
   sudo systemctl start docker
   
   # macOS (via Docker Desktop)
   ```

2. **MinIO**: Either running via Docker or using the run command provided earlier
   ```bash
   # Check if MinIO is running
   docker ps | grep minio
   
   # If not running, start it
   docker start minio
   ```

3. **CouchDB**:
   ```bash
   # Linux
   sudo systemctl start couchdb
   
   # macOS
   brew services start couchdb
   ```

**Starting the Backend**:
```bash
# From the backend directory
./mvnw spring-boot:run

# The API should now be available at http://localhost:7000
```

**Starting the Frontend**:
```bash
# From the frontend directory
ng serve

# The frontend should now be available at http://localhost:4200
```

✅ **Pre-Launch Checklist**

Before running the application, verify that all services are running:

- [ ] Docker is running
- [ ] MinIO container is running (`docker ps` should show minio container)
- [ ] CouchDB service is running
- [ ] All credentials have been updated in the code:
  - [ ] MinIO Access Key updated
  - [ ] MinIO Secret Key updated
  - [ ] CouchDB username updated
  - [ ] CouchDB password updated
- [ ] Docker images built successfully:
  - [ ] post-processing image
  - [ ] visualization image
- [ ] Python dependencies installed
- [ ] Backend dependencies installed
- [ ] Frontend dependencies installed
- [ ] CouchDB design documents installed

Now you can access the application at `http://localhost:4200` 🎉
