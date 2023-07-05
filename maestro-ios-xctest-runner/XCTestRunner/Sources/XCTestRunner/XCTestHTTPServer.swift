import FlyingFox
import Foundation

enum Route: String, CaseIterable {
    case subTree
    case runningApp
    case swipe
    case swipeV2
    case inputText
    case touch
    case screenshot
    case isScreenStatic
    case pressKey
    case pressButton
    case eraseText
    case deviceInfo
    case setPermissions
    case viewHierarchy
    
    func toHTTPRoute() -> HTTPRoute {
        return HTTPRoute(rawValue)
    }
}

public struct XCTestHTTPServer {
    public init() {}
    
    public func start() async throws {
        let port = ProcessInfo.processInfo.environment["PORT"]?.toUInt16()
        let server = HTTPServer(address: .loopback(port: port ?? 22087))

        XCUIApplicationProcessSwizzler.setup
        
        for route in Route.allCases {
            let handler = await RouteHandlerFactory.createRouteHandler(route: route)
            await server.appendRoute(route.toHTTPRoute(), to: handler)
        }
        
        try await server.start()
    }
}