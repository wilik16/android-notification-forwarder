#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# App package name from build.gradle.kts
PACKAGE_NAME="id.wilik.notificationforwarder"
MAIN_ACTIVITY="$PACKAGE_NAME/.MainActivity"

# Function to print messages
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to check if adb is connected to any device
check_device() {
    local devices=$(adb devices | grep -v "List" | grep "device\|unauthorized")
    if [ -z "$devices" ]; then
        print_message "No devices connected. Please connect a device first." "$RED"
        print_message "You can connect using: adb connect IP:PORT" "$YELLOW"
        exit 1
    fi
    
    if echo "$devices" | grep -q "unauthorized"; then
        print_message "Unauthorized device found. Please check the device and accept the debugging prompt." "$RED"
        exit 1
    fi
    
    print_message "Device connected successfully!" "$GREEN"
}

# Function to build the app
build_app() {
    print_message "Building the app..." "$YELLOW"
    if ./gradlew assembleDebug; then
        print_message "Build successful!" "$GREEN"
        return 0
    else
        print_message "Build failed!" "$RED"
        return 1
    fi
}

# Function to install the app
install_app() {
    print_message "Installing the app..." "$YELLOW"
    if ./gradlew installDebug; then
        print_message "Installation successful!" "$GREEN"
        return 0
    else
        print_message "Installation failed!" "$RED"
        return 1
    fi
}

# Function to launch the app
launch_app() {
    print_message "Launching the app..." "$YELLOW"
    if adb shell am start -n "$MAIN_ACTIVITY"; then
        print_message "App launched successfully!" "$GREEN"
        return 0
    else
        print_message "Failed to launch the app!" "$RED"
        return 1
    fi
}

# Function to show logs
show_logs() {
    local filter="$1"
    print_message "Showing logs. Press Ctrl+C to stop..." "$YELLOW"
    
    # Clear the logcat buffer first
    adb logcat -c
    
    if [ -z "$filter" ]; then
        print_message "Showing logs for package: $PACKAGE_NAME" "$YELLOW"
        # Use -f to follow the logs and --pid to show only logs from our app
        logcat $PACKAGE_NAME
    else
        print_message "Showing logs with filter: $filter (only from $PACKAGE_NAME)" "$YELLOW"
        # Combine package filter with user filter and follow
        logcat $PACKAGE_NAME | grep -i "$filter"
    fi
}

function logcat {
  pkg="$1"
  shift
  if [ -z "$pkg" ]; then
    >&2 echo 'Usage: logcat pkg ...'
    return 1
  fi

  uid="$(adb shell pm list package -U $pkg | sed 's/.*uid://')"
  if [ -z "$uid" ]; then
    >&2 echo "pkg '$pkg' not found"
    return 1
  fi

  adb logcat --uid="$uid" "$@"
}

# Main script
main() {
    local show_logs=false
    local launch=false
    local log_filter=""
    
    # Parse command line arguments
    while [[ "$#" -gt 0 ]]; do
        case $1 in
            -l|--logs)
                show_logs=true
                # Check if next argument exists and doesn't start with -
                if [[ -n "$2" && ! "$2" =~ ^- ]]; then
                    log_filter="$2"
                    shift
                fi
                ;;
            -r|--run) launch=true ;;
            -h|--help)
                echo "Usage: $0 [-l|--logs [filter]] [-r|--run] [-h|--help]"
                echo "  -l, --logs    Show logs after deployment. Optionally specify a filter string"
                echo "  -r, --run     Launch the app after installation"
                echo "  -h, --help    Show this help message"
                echo ""
                echo "Examples:"
                echo "  $0 -l                     # Show logs filtered by package name"
                echo "  $0 -l \"NotificationListener\" # Show logs filtered by specific term"
                echo "  $0 -r -l \"ERROR\"           # Launch app and show error logs"
                exit 0
                ;;
            *) print_message "Unknown parameter: $1" "$RED"; exit 1 ;;
        esac
        shift
    done
    
    # Check if device is connected
    check_device
    
    # Build and install
    build_app || exit 1
    install_app || exit 1
    
    # Launch if requested
    if [ "$launch" = true ]; then
        launch_app || exit 1
    fi
    
    # Show logs if requested
    if [ "$show_logs" = true ]; then
        show_logs "$log_filter"
    fi
}

# Run the script
main "$@" 