package io.horizen.services;

import java.util.List;
import java.util.Map;

public interface NscService {

    Map<String, List<String>> getNscOwnerships(String scAddress) throws Exception ;

    List<String> getNscOwnerScAddresses() throws Exception ;
}
