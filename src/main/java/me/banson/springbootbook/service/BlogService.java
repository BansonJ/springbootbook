package me.banson.springbootbook.service;

import lombok.RequiredArgsConstructor;
import me.banson.springbootbook.domain.Article;
import me.banson.springbootbook.domain.User;
import me.banson.springbootbook.dto.ArticleDto;
import me.banson.springbootbook.repository.BlogRepository;
import me.banson.springbootbook.repository.QRepository.BlogRepositoryImpl;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final FileStore fileStore;

    private final BlogRepository blogRepository;
    private final BlogRepositoryImpl qBlogRepository;

    @Transactional
    public Article save(ArticleDto request, User user) throws IOException {
        Article article = Article.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(user.getNickname())
                .originalFileName(request.getOriginalFileName().getOriginalFilename())
                .storeFileName(fileStore.storeFiles(request.getOriginalFileName()))
                .user(user)
                .build();
        return blogRepository.save(article);
    }

    public Page<Article> findByTitleContaining(Pageable pageable, String search) {
        return blogRepository.findByTitleContaining(PageRequest.of(pageable.getPageNumber()-1,pageable.getPageSize(), Sort.by("createdAt").descending()), search);
    }

    public Article findById(long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found: " + id));
    }

    public void delete(long id) throws IOException {
        Article article = findById(id);
        UrlResource resource = new UrlResource("file:" +
                fileStore.getFullPath(article.getStoreFileName()));
        resource.getFile().delete();

        blogRepository.deleteById(id);
    }

    @Transactional
    public Article update(ArticleDto request) throws IOException {
        Article article = blogRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("not found: " + request.getId()));

        if (request.getOriginalFileName() != null) {
            article.update(request.getTitle(), request.getContent(), request.getOriginalFileName().getOriginalFilename(), fileStore.storeFiles(request.getOriginalFileName()));
        } else {
            article.update(request.getTitle(), request.getContent());
        }

        return article;
    }

    public Page<Article> findMyArticle(String name, Pageable pageable, String search) {
        return qBlogRepository.findMyArticle(name, PageRequest.of(pageable.getPageNumber()-1, pageable.getPageSize()), search);
    }

    @Transactional
    public void removeFile(Long id) {
        qBlogRepository.removeFile(id);
    }
}
