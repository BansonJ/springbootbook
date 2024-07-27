package me.banson.springbootbook.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.banson.springbootbook.domain.Article;
import me.banson.springbootbook.domain.Comment;
import me.banson.springbootbook.domain.User;
import me.banson.springbootbook.dto.ArticleDto;
import me.banson.springbootbook.service.BlogService;
import me.banson.springbootbook.service.CommentService;
import me.banson.springbootbook.service.FileStore;
import me.banson.springbootbook.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BlogViewController {

    private final BlogService blogService;
    private final UserService userService;
    private final CommentService commentService;
    private final FileStore fileStore;
    private final String PR;
    private final int ppap;
    private final int ppappp;

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return authentication.isAuthenticated();
    }

    @ModelAttribute("name")
    private String userName(Principal principal) {
        if (isAuthenticated()) {
            String name = userService.findUser(principal).getNickname();
            return name;
        }
            return null;
    }

    //전체 게시물 조회
    @GetMapping("/articles")
    public String getArticles(Model model, @PageableDefault(size = 3, page = 1) Pageable pageable,
                              @RequestParam(required = false, defaultValue = "") String search, Principal principal) {
        Page<Article> articles = blogService.findByTitleContaining(pageable, search);
        model.addAttribute("articles", articles);

        int nowPage = pageable.getPageNumber(); //1
        int firstPage = (nowPage-1) / 10 * 10 + 1;
        int lastPage = (nowPage-1) / 10 * 10 + 10;

        if (lastPage > articles.getTotalPages()) {
            lastPage = articles.getTotalPages();
        }

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("search", search);
        model.addAttribute("firstPage", firstPage);
        model.addAttribute("totalPage", articles.getTotalPages());

        return "articleList";
    }

    //게시물 하나 조회
    @GetMapping("/articles/{id}")
    public String getArticle(@PathVariable Long id, Model model, @PageableDefault(sort = "id", size = 3, page = 1) Pageable pageable) {
        Article article = blogService.findById(id);
        model.addAttribute("article", article);
        Page<Comment> comments = commentService.findByArticleId(pageable, String.valueOf(id));
        model.addAttribute("comments", comments);

        int nowPage = pageable.getPageNumber(); //1
        int firstPage = (nowPage-1) / 10 * 10 + 1;
        int lastPage = (nowPage-1) / 10 * 10 + 10;

        if (lastPage > comments.getTotalPages()) {
            lastPage = comments.getTotalPages();
        }

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("firstPage", firstPage);
        model.addAttribute("totalPage", comments.getTotalPages());

        return "article";
    }

    @GetMapping("/new-article")
    public String newArticle(@RequestParam(required = false) Long id, Model model,Principal principal) throws IOException {
        if (id == null) {
            model.addAttribute("article", new ArticleDto());
        } else {
            Article article = blogService.findById(id);
            if(!article.getAuthor().equals(this.userName(principal))){
                return "redirect:/articles";
            }
            model.addAttribute("article", new ArticleDto(article));
        }
        return "newArticle";
    }

    @PostMapping("/new-article")
    public String newArticle(@ModelAttribute("article") ArticleDto articleDto, Model model, Principal principal) throws IOException {
        Article article;
        User user = userService.findUser(principal);

        if (articleDto.getId() != null) {
            article = blogService.update(articleDto);
        }
        else {
            article = blogService.save(articleDto, user);
        }

        model.addAttribute("article", article);

        return "redirect:/articles/" + article.getId();
    }

    @GetMapping("/attach/{id}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long id)
            throws MalformedURLException {
        Article article = blogService.findById(id);
        String storeFileName = article.getStoreFileName();
        String originalFileName = article.getOriginalFileName();

        UrlResource resource = new UrlResource("file:" +
                fileStore.getFullPath(storeFileName));

        String encodedUploadFileName = UriUtils.encode(originalFileName,
                StandardCharsets.UTF_8);

        String contentDisposition = "attachment; filename=\"" +
                encodedUploadFileName + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    @GetMapping("/deleteAttach/{id}")
    public String deleteAttach(@PathVariable Long id)
            throws IOException {
        Article article = blogService.findById(id);

        UrlResource resource = new UrlResource("file:" +
                fileStore.getFullPath(article.getStoreFileName()));
        resource.getFile().delete();

        blogService.removeFile(id);

        return "redirect:/new-article?id=" + id;
    }

    @GetMapping("delete-article")
    public String deleteArticle(@RequestParam Long id) throws IOException {
        blogService.delete(id);
        commentService.deleteByArticleId(String.valueOf(id));
        return "redirect:/articles";
    }

    @GetMapping("/myArticles")
    public String myArticles(Model model, Principal principal, @PageableDefault(sort = "id", size = 3, page = 1) Pageable pageable,
                             @RequestParam(required = false, defaultValue = "") String search) {
        String name = this.userName(principal);
        Page<Article> articles = blogService.findMyArticle(name, pageable, search);
        model.addAttribute("articles", articles);

        int nowPage = pageable.getPageNumber(); //0
        int firstPage = (nowPage-1) / 10 * 10 + 1;
        int lastPage = (nowPage-1) / 10 * 10 + 10;

        if (lastPage > articles.getTotalPages()) {
            lastPage = articles.getTotalPages();
        }

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("search", search);
        model.addAttribute("firstPage", firstPage);
        model.addAttribute("totalPage", articles.getTotalPages());
        return "myArticles";
    }
}
