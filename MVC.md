## MVC project structure (for each MS)

### Controllers
All the controllers should be put in a single class file.

### Views
Contains .html files.

### Models
Contains a class for each model to be used. (labeled with __@Document__). __dtos__ contains a data class for each dto.

### Config
Contains the configuration files for security and db (if needed).

### Miscellaneous
Contains utils files (like enums).

### Services
Contains one class file for each service is needed (labeled with __@Service__). Within the class file should be present also the relative interface from which the class inherits.

### Repositories
Contains one interface for each repository needed. (labeled with __@Repository__)
