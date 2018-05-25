# Used Book Buy and Rental Website for SDSU

The key idea behind this project is to develop a website for students to rent or purchase used books. The scope of this project is to develop a full working prototype. This will provide a platform for students to make the best out of their old books if they are not longer using it. The project is divided into 2 parts
  - One is a portal with admin privilages that manages the inventory (used books).
  - Second one is the actual store like portal through which students can rent/buy books.

The website implements a web scraping logic to determine the price of each book by looking into some of the popular book inventory websites. This self determined prices can be manipulated through the admin portal. The targeting audience for this initial version of this website are students of San Diego State University.  

### Installation

The application run on tomcat with java 8. Install MySQL as database and update the config file with DB credentials. We will also need Redis to run the application as caching is implemented with it. All other development dependencies are managed in maven. The Front-end is developed using Angular 5 and the angualar cli is used to manage project structure. ```npm install``` on the root directory of admin-portal and store-portal will make it ready for execution.

### Usage

```
public class HelloWorld {

    // method main(): ALWAYS the APPLICATION entry point
    public static void main (String[] args) {
	System.out.println ("Hello World!");
    }
}
```

```
<div ng-app="myApp" ng-controller="myCtrl">
    Name: <input ng-model="name">
</div>

<script>
var app = angular.module('myApp', []);
app.controller('myCtrl', function($scope) {
    $scope.name = "John Doe";
});
```
### Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

License
----

**Free Software, Hell Yeah!**