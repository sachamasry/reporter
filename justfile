# Reporter project justfile                                                                                            

alias uber := compile

[private]                                                                                                               
@help:                                                                                                                  
    just --list  

# Download and install project dependencies
get-dependencies:
    @echo "==> Getting Reporter dependencies..."
    lein deps
    @echo "--> Successfully downloaded Reporter dependencies"

# Compile application Uberjar (standalone JAR file)
compile: clean
    @echo "==> Compiling application Uberjar..."
    lein uberjar
    @echo "--> Successfully compiled Reporter"

# Clean build
clean:
    @echo "==> Cleaning application build..."
    lein clean
    @echo "--> Successfully cleaned application build directory"

# Launch REPL
repl:
    lein repl
