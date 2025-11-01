A Load Balancer is a cloud service that distributes network traffic across multiple servers to
improve performance, reliability, and scalability. In the context of the COMP20081 project, the
load balancer will play a crucial role in managing the workload across the four file storage
containers within the simulated cloud infrastructure.

Key functions of a load balancer in this coursework:
• Receive incoming requests (e.g., file uploads, downloads) and distribute them evenly to the
available file storage containers.
• Help to prevent any single server from becoming overloaded, ensuring better overall
performance and responsiveness.
• Redirect traffic to the remaining healthy containers, minimising downtime and ensuring
service continuity.
• The load balancer can be configured to handle increasing workloads by adding more file
storage containers to the infrastructure. This feature allows the system to scale up or down
based on demand.

The load balancer will employ at least three of the following scheduling algorithms: First-Come,
First-Served (FCFS), Shortest-Job-Next (SJN), Priority Scheduling, Shortest Remaining Time,
Round Robin (RR) and Multiple-Level Queues. The scheduling algorithms will be used to optimise
resource allocation and handle concurrent file uploads, downloads, and deletions.
Users will access the system through a Java and JavaFX-based portal. Standard users can create
accounts, update their details, upload, download, share with read and/or write permissions and
delete files. Admin users have additional privileges, including creating, updating, and deleting users
and promoting standard users to admin.

To simulate real-world scenarios, the load balancer will introduce artificial delays ranging from 30
to 90 seconds to emulate the varying response times experienced by multiple users accessing cloud
resources. Additionally, file containers will be locked during upload, download, and deletion
operations to prevent concurrent access and ensure data integrity.

Key functionalities:

- User Management: The system implements user authentication and
authorisation mechanisms to ensure that only authorised users can access files

- Session Management: Implemented session management to keep users logged in
during their interactions with the system.

- User Roles: Implemented different user roles (standard and admin) with varying
levels of access and control over files.

- File Management:
• Create new file: allowing the user to add new content
• Update file content
• Delete file

- Access Control Lists (ACLs): Enable users to set permissions on files for different
users (e.g., read and/or write).

- File Sharing: Allow users to share files with other users and set specific
permissions (read/write).

- Encryption/Decryption: Use strong encryption algorithms (e.g., AES) to encrypt
passwords and file chunks before distribution and decrypt them upon retrieval.

- Load Balancer Functionality: The load balancer distributes incoming
requests evenly among the file storage containers

- Traffic Management: The load balancer handles traffic spikes without
degrading performance, maintaining a responsive user experience.

- Health Check Mechanism: Health checks for file storage containers to
ensure only healthy containers receive traffic.

- Artificial Delay Simulation: Artificial delays ranging from 30 to 90
seconds to simulate real-world latency.

- Local SQLite Database: Implement a local SQLite database to store user session
data and temporary file metadata, ensuring fast access and reduced latency.

- Remote MySQL Database: Remote MySQL database to store user
profiles, file metadata, and access control lists, enabling centralised management.

- User Interface: Provide a user-friendly JavaFX interface for users to interact with
the system intuitively.

- Remote Terminal Emulation: Allow users to remotely access the containers using
the JSch library, enabling command execution within containers.

- File Chunking: Develop a file chunking mechanism to split large files into smaller,
manageable parts.

- Distributed Docker Containers: Utilise Docker containers to distribute and store
encrypted file chunks across a scalable infrastructure.
