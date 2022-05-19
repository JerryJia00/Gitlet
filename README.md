# Gitlet
This project is about the implemenation of a simple version of Git. The core part is in `Repository.java` which contains all the functions. (This is a class project of the data structure class at UC Berkeley)

# Run
In the same level of the gitlet folder, run the followings to compile:
```
make
```
or
```
javac gitlet/*.java
```
All the commands have the form of
```
java gitlet.Main [command]
```
To run the program, make sure to run `java gitlet.Main init` first.
Here's a list of all the commands:
```
init
add [file name] 
commit [message]
rm [file name]
log
status
branch [branch name]
checkout -- [filename]
checkout [commit id] -- [file name]
checkout [branch name]
reset [commit id]
merge [branch name]
```
