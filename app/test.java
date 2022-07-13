private static final int REQUEST_CODE = 1337;
private static final String REDIRECT_URI = "yourcustomprotocol://callback";

        AuthorizationRequest.Builder builder =
        new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);