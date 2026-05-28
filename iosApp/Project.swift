import ProjectDescription

let kotlinBuildScript = """
if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES"
  exit 0
fi
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
"""

let project = Project(
    name: "iosApp",
    settings: .settings(
        base: [
            "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
            "SWIFT_VERSION": "5.0"
        ],
        configurations: [
            .debug(name: "Debug"),
            .release(name: "Release")
        ]
    ),
    targets: [
        .target(
            name: "WorkshopSample",
            destinations: .iOS,
            product: .app,
            bundleId: "ru.yandex.workshop.WorkshopSample",
            deploymentTargets: .iOS("16.0"),
            infoPlist: "./WorkshopSample/Info.plist",
            sources: ["./WorkshopSample/**"],
            resources: [
                "./WorkshopSample/Assets.xcassets",
                "./WorkshopSample/LaunchScreen.storyboard",
                "./WorkshopSample/Preview Content/**"
            ],
            entitlements: "./WorkshopSample/WorkshopSample.entitlements",
            scripts: [
                .pre(
                    script: kotlinBuildScript,
                    name: "Kotlin",
                    basedOnDependencyAnalysis: false
                )
            ],
            dependencies: [
                .external(name: "YMK"),
                .sdk(name: "CoreLocation", type: .framework),
                .sdk(name: "DeviceCheck", type: .framework),
                .sdk(name: "SystemConfiguration", type: .framework)
            ],
            settings: .settings(
                base: [
                    "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
                    "ASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME": "AccentColor",
                    "DEVELOPMENT_ASSET_PATHS": "\"./WorkshopSample/Preview Content\"",
                    "LD_RUNPATH_SEARCH_PATHS": "@executable_path/Frameworks",
                    "OTHER_LDFLAGS": "$(inherited) -ObjC",
                    "TARGETED_DEVICE_FAMILY": "1,2"
                ]
            )
        )
    ]
)
