package com.back.dev.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("!prod")
@Service
public class DevTestQuizService {

}
