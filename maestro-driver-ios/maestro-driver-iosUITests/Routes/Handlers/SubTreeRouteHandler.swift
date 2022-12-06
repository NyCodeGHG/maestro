import FlyingFox
import XCTest
import os

class SubTreeRouteHandler: RouteHandler {
    
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "SubTreeRouteHandler")
    
    func handle(request: HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        guard let appId = request.query["appId"] else {
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest)
        }
        let viewHierarchyDictionaryResult = await MainActor.run { () -> [XCUIElement.AttributeName : Any]? in
            do {
                return try XCUIApplication(bundleIdentifier: appId).snapshot().dictionaryRepresentation
            } catch let error {
                let message = error.localizedDescription
                logger.error("Snapshot failure, cannot return view hierarchy due to \(message)")
                return nil
            }
        }
        guard let viewHierarchyDictionary = viewHierarchyDictionaryResult else {
            let jsonString = """
             { "errorMessage" : "Snapshot failure while getting view hierarchy" }
            """
            let errorData = Data(jsonString.utf8)
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        guard let hierarchyJsonData = try? JSONSerialization.data(
            withJSONObject: viewHierarchyDictionary,
            options: .prettyPrinted
        ) else {
            logger.info("Serialization of view hierarchy failed \(viewHierarchyDictionary.debugDescription)")
            let jsonString = """
             { "errorMessage" : "Not able to serialize the view hierarchy response" }
            """
            return HTTPResponse(statusCode: .badRequest, body: Data(jsonString.utf8))
        }
        return HTTPResponse(statusCode: .ok, body: hierarchyJsonData)
    }
}
