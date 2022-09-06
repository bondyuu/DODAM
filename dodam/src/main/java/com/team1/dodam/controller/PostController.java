package com.team1.dodam.controller;

import com.team1.dodam.dto.request.PostRequestDto;
import com.team1.dodam.dto.response.PostSearchResponseDto;
import com.team1.dodam.dto.response.ResponseDto;
import com.team1.dodam.domain.UserDetailsImpl;
import com.team1.dodam.service.PostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Api(tags = {"게시글 전체 조회 및 검색/생성/상세 조회/수정/삭제/찜하기"})
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    // 게시글 전체 조회 및 검색
    // 검색어, 카테고리에 해당하는 게시글을 생성시간 기준 역순으로 조회
    @ApiOperation(value = "게시글 전체 조회 및 검색")
    @GetMapping
    public ResponseDto<?> searchPosts(@RequestParam(required = false) String searchValue,
                                      @RequestParam(required = false) String category,
                                      @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<PostSearchResponseDto> posts = postService.searchPosts(searchValue, category, pageable).map(PostSearchResponseDto::from);
//        List<Integer> barNumbers = paginationService.getPaginationBarNumbers(pageable.getPageNumber(), posts.getTotalPages());

        return ResponseDto.success(posts);
    }

    // 게시글 생성
    @ApiOperation(value = "게시글 생성 메소드")
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseDto<?> sharingPosting(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @Valid @RequestPart(value = "requestDto") PostRequestDto requestDto,
                                         @RequestPart(value = "imageFileList", required = false) List<MultipartFile> imageFileList) throws IOException {
        return postService.create(userDetails, requestDto, imageFileList);
    }

    //게시글 상세 조회


    //게시글 수정


    //게시글 삭제


    // 게시글 찜하기
    @ApiOperation(value = "게시글 찜하기 메소드")
    @PostMapping("/{postId}/pick")
    public ResponseDto<?> postPick(@PathVariable(name = "postId") Long postId,
                                   @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {
        return postService.postPick(postId, userDetails);
    }

    @PostMapping("/posting")
    public ResponseDto<?> post(CreateRequestDto requestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {
        return postService.post(requestDto, userDetails);
    }
}
